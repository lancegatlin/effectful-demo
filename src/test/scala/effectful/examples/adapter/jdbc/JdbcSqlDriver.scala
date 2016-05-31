package effectful.examples.adapter.jdbc

import java.util.concurrent.ConcurrentHashMap

import effectful._
import effectful.examples.effects.sql.SqlDriver._
import effectful.examples.effects.sql._
import effectful.examples.pure.uuid.UUIDService

object JdbcSqlDriver {
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
  getConnectionFromPool: () => java.sql.Connection,
  uuids: UUIDService[Id]
) extends SqlDriver[Id] {
  import JdbcSqlDriver._
  import JdbcSqlDriverOps._

  def getJdbcConnection()(implicit context: Context) : java.sql.Connection = {
    context match {
      case Context.AutoCommit =>
        getConnectionFromPool()
      case Context.InTransaction(transactionId) =>
        val internalTransaction = transactions(transactionId)
        internalTransaction.connection
    }
  }

  // Transaction

  val transactions = new ConcurrentHashMap[Symbol,InternalTransaction]()

  override def beginTransaction(): Id[Context.InTransaction] = {
    // Note: getting another connection from pool per transaction to avoid dealing with "in transaction"
    // state of sharing main auto commit connection
    val connection = getConnectionFromPool()
    connection.setAutoCommit(false)
    val internalTransaction = InternalTransaction(
      id = genId(),
      connection = connection
    )
    transactions.put(internalTransaction.id, internalTransaction)
    Context.InTransaction(internalTransaction.id)
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
  ): Id[PreparedStatementId] = {
    val jdbcConnection = getJdbcConnection()
    val jdbcPreparedStatement = jdbcConnection.prepareStatement(statement)
    val id = genId()
    preparedStatements.put(id,InternalPreparedStatement(
      id = id,
      statement = statement,
      jdbcPreparedStatement = jdbcPreparedStatement,
      jdbcConnection = jdbcConnection
    ))
    id
  }

  override def executePreparedUpdate(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ): Id[Int] = {
    // todo: think about race conditions here
    val internalPreparedStatement = preparedStatements(preparedStatementId)
    prepareBatches(internalPreparedStatement.jdbcPreparedStatement,rows)
    val updateCount = internalPreparedStatement.jdbcPreparedStatement.executeUpdate()
    closePreparedStatement(internalPreparedStatement)(internalPreparedStatement.context)
    updateCount
  }

  override def executePreparedQuery(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ): Id[Cursor] = {
    val internalPreparedStatement = preparedStatements(preparedStatementId)
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

  override def getCursorMetadata(cursorId: CursorId): Id[CursorMetadata] =
    cursors(cursorId).getCursorMetadata

  override def closeCursor(cursorId: CursorId): Id[Unit] = {
    cursors(cursorId).close()
    cursors.remove(cursorId)
  }

  override def nextCursor(cursorId: CursorId): Id[Cursor] =
    cursors(cursorId).next()

  // Utility methods

  def genId() : Symbol = Symbol(uuids.toBase64(uuids.gen()))
}
