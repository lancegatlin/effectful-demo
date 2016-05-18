package effectful.examples.effects.sql.jdbc

import java.sql.{Connection => _, PreparedStatement => _, _}
import java.util.concurrent.ConcurrentHashMap
import effectful._
import effectful.examples.effects.sql._
import SqlDriver._
import effectful.examples.pure.UUIDService

object JdbcSqlDriver {
  case class ConnectionInfo(
    url: String,
    username: String,
    password: String
  )

  case class InternalPreparedStatement(
    id: Symbol,
    statement: String,
    jdbcPreparedStatement: java.sql.PreparedStatement,
    jdbcConnection: java.sql.Connection
  )(implicit
    val context: Context
  )

  case class InternalTransaction(
    id: Symbol,
    connection: java.sql.Connection
  )

}

class JdbcSqlDriver(
  getConnectionFromPool: JdbcSqlDriver.ConnectionInfo => java.sql.Connection,
  uuids: UUIDService[Id]
) extends SqlDriver[Id] {
  import JdbcSqlDriver._
  import JdbcSqlDriverOps._

  // Connection
  val connectionInfos = new ConcurrentHashMap[Symbol,ConnectionInfo]()

  override def getConnectionPool(
    url: String,
    username: String,
    password: String
  ): Id[ConnectionPool] = {
    // Note: since using connection pooling, just save connection info here
    val connectionInfo = ConnectionInfo(url,username,password)
    val connectionPool = ConnectionPool(
      id = genId()
    )
    connectionInfos.put(connectionPool.id, connectionInfo)
    connectionPool
  }

  override def closeConnectionPool(connectionPool: ConnectionPool): Id[Unit] =
    // Note: not cleaning up dangling here depending on caller to do the right thing
    connectionInfos.remove(connectionPool.id)

  def getJdbcConnection()(implicit context: Context) : java.sql.Connection = {
    context match {
      case Context.AutoCommit(connectionPool) =>
        getConnectionFromPool(connectionInfos(connectionPool.id))
      case Context.InTransaction(transactionId, _) =>
        val internalTransaction = transactions(transactionId)
        internalTransaction.connection
    }
  }


  // Transaction

  val transactions = new ConcurrentHashMap[Symbol,InternalTransaction]()

  override def beginTransaction()(implicit context: Context.AutoCommit): Id[Context.InTransaction] = {
    // Note: getting another connection from pool per transaction to avoid dealing with "in transaction"
    // state of sharing main auto commit connection
    val connection = getConnectionFromPool(connectionInfos(context.connectionPool.id))
    connection.setAutoCommit(false)
    val internalTransaction = InternalTransaction(
      id = genId(),
      connection = connection
    )
    transactions.put(internalTransaction.id, internalTransaction)
    Context.InTransaction(
      id = internalTransaction.id,
      connectionPool = context.connectionPool
    )
  }

  def closeTransaction(internalTransaction: InternalTransaction) = {
    // Note: releases connection back to connection pool
    internalTransaction.connection.close()
    // todo: could have race condition here
    transactions.remove(internalTransaction.id)
  }

  override def rollback()(implicit context: Context.InTransaction): Id[Unit] = {
    val internalTransaction = transactions(context.id)
    internalTransaction.connection.rollback()
    closeTransaction(internalTransaction)
  }

  override def commit()(implicit context: Context.InTransaction): Id[Unit] = {
    val internalTransaction = transactions(context.id)
    internalTransaction.connection.commit()
    closeTransaction(internalTransaction)
  }

  // Prepared statement

  val preparedStatements = new ConcurrentHashMap[Symbol,InternalPreparedStatement]()

  def closePreparedStatement(internalPreparedStatement: InternalPreparedStatement)(implicit context: Context) = {
    internalPreparedStatement.jdbcPreparedStatement.close()
    if(context.isInTransaction == false) {
      internalPreparedStatement.jdbcConnection.close()
    }
    // todo: could have race condition here
    preparedStatements.remove(internalPreparedStatement.id)
  }

  override def prepare(
    statement: String
  )(implicit
    context: Context
  ): Id[PreparedStatement] = {
    val jdbcConnection = getJdbcConnection()
    val jdbcPreparedStatement = jdbcConnection.prepareStatement(statement)
    val id = genId()
    preparedStatements.put(id,InternalPreparedStatement(
      id = id,
      statement = statement,
      jdbcPreparedStatement = jdbcPreparedStatement,
      jdbcConnection = jdbcConnection
    ))
    PreparedStatement(
      id = id,
      statement = statement
    )
  }

  override def executePreparedUpdate(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  ): Id[Int] = {
    // todo: think about race conditions here
    val internalPreparedStatement = preparedStatements(preparedStatement.id)
    prepareBatches(internalPreparedStatement.jdbcPreparedStatement,rows)
    val updateCount = internalPreparedStatement.jdbcPreparedStatement.executeUpdate()
    closePreparedStatement(internalPreparedStatement)(internalPreparedStatement.context)
    updateCount
  }

  override def executePreparedQuery(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  ): Id[Cursor] = {
    val internalPreparedStatement = preparedStatements(preparedStatement.id)
    prepareBatches(internalPreparedStatement.jdbcPreparedStatement,rows)
    createCursor(
      resultSet = internalPreparedStatement.jdbcPreparedStatement.executeQuery(),
      onClose = { () => closePreparedStatement(internalPreparedStatement)(internalPreparedStatement.context) }
    )
  }

  // Immediate execution

  override def executeQuery(
    statement: String
  )(implicit
    context: Context
  ): Id[Cursor] = {
    val jdbcConnection = getJdbcConnection()
    val jdbcStatement = jdbcConnection.createStatement()
    createCursor(
      resultSet = jdbcStatement.executeQuery(statement),
      onClose = { () =>
        jdbcStatement.close()
        if(context.isInTransaction == false) {
          jdbcConnection.close()
        }
      }
    )
  }

  override def executeUpdate(
    statement: String
  )(implicit
    context: Context
  ): Id[Int] = {
    val jdbcConnection = getJdbcConnection()
    val jdbcStatement = jdbcConnection.createStatement()
    val updateCount = jdbcStatement.executeUpdate(statement)
    jdbcStatement.close()
    if(context.isInTransaction == false) {
      jdbcConnection.close()
    }
    updateCount
  }

  // Cursor

  val cursors = new ConcurrentHashMap[Symbol,JdbcResultSetCursor]()

  def createCursor(resultSet: java.sql.ResultSet, onClose: () => Unit) : Cursor = {
    val internalCursor = JdbcResultSetCursor(
      id = genId(),
      resultSet = resultSet,
      onClose = onClose
    )
    cursors.put(internalCursor.id, internalCursor)
    internalCursor.next()
  }

  override def getCursorMetadata(cursor: Cursor): Id[CursorMetadata] =
    cursors(cursor.id).getCursorMetadata

  override def closeCursor(cursor: Cursor): Id[Unit] = {
    cursors(cursor.id).close()
    cursors.remove(cursor.id)
  }

  override def nextCursor(cursor: Cursor): Id[Cursor] =
    cursors(cursor.id).next()

  // Utility methods

  def genId() : Symbol = Symbol(uuids.toBase64(uuids.gen()))
}
