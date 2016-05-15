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

  case class InternalTransaction(
    id: Symbol,
    connection: java.sql.Connection
  )

}

class JdbcSqlDriver(
  getConnectionFromPool: JdbcSqlDriver.ConnectionInfo => java.sql.Connection,
  getPreparedStatementFromPool: String => java.sql.PreparedStatement,
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
    val connectionInfo = ConnectionInfo(url,username,password)
    val connectionPool = ConnectionPool(
      id = genId()
    )
    connectionInfos.put(connectionPool.id, connectionInfo)
    connectionPool
  }

  override def closeConnectionPool(connectionPool: ConnectionPool): Id[Unit] =
    connectionInfos.remove(connectionPool.id)


  // Transaction

  val transactions = new ConcurrentHashMap[Symbol,InternalTransaction]()
  override def beginTransaction()(implicit context: Context.AutoCommit): Id[Context.InTransaction] = {
    val connection = getConnectionFromPool(connectionInfos(context.connectionPool.id))
    // Note: getting another connection from pool for transaction to avoid dealing with "in transaction"
    // state of main auto commit connection
    connection.setAutoCommit(false)
    val internalTransaction = InternalTransaction(
      id = genId(),
      connection = connection
    )
    Context.InTransaction(
      id = internalTransaction.id,
      connectionPool = context.connectionPool
    )
  }

  override def rollback()(implicit context: Context.InTransaction): Id[Unit] = {
    val connection = transactions(context.id).connection
    connection.rollback()
    connection.close() // return to pool
    transactions.remove(context.id)
  }

  override def commit()(implicit context: Context.InTransaction): Id[Unit] = {
    val connection = transactions(context.id).connection
    connection.commit()
    // Note: releases connection back to connection pool
    connection.close()
    transactions.remove(context.id)
  }

  // Prepared statement

  override def prepare(
    statement: String
  )(implicit
    context: Context
  ): Id[PreparedStatement] = {
    // Prime for later so we don't have to hold a connection
    getPreparedStatementFromPool(statement).close()
    PreparedStatement(
      id = genId(), // Note: id not really used in this impl
      statement = statement
    )
  }

  override def executePreparedUpdate(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ): Id[Int] = {
    // Note: should already be in pool
    val ps = getPreparedStatementFromPool(preparedStatement.statement)
    prepareBatches(ps,rows)
    val updateCount = ps.executeUpdate()
    ps.close()
    updateCount
  }

  override def executePreparedQuery(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ): Id[Cursor] = {
    // Note: should already be in pool
    val ps = getPreparedStatementFromPool(preparedStatement.statement)
    prepareBatches(ps,rows)
    ps.closeOnCompletion()
    val internalCursor = ResultSetCursor(
      id = genId(),
      resultSet = ps.executeQuery()
    )
    cursors.put(internalCursor.id, internalCursor)
    internalCursor.next()
  }

  // Immediate execution

  override def executeQuery(
    statement: String
  )(implicit
    context: Context
  ): Id[Cursor] = {
    val connection = getConnectionFromPool(connectionInfos(context.connectionPool.id))
    val jdbcStatement = connection.createStatement()
    val internalCursor = ResultSetCursor(
      id = genId(),
      jdbcStatement.executeQuery(statement)
    )
    cursors.put(internalCursor.id, internalCursor)
    internalCursor.next()
  }

  override def executeUpdate(
    statement: String
  )(implicit
    context: Context
  ): Id[Int] = {
    val connection = getConnectionFromPool(connectionInfos(context.connectionPool.id))
    val jdbcStatement = connection.createStatement()
    val updateCount = jdbcStatement.executeUpdate(statement)
    connection.close()
    updateCount
  }

  // Cursor

  val cursors = new ConcurrentHashMap[Symbol,ResultSetCursor]()

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
