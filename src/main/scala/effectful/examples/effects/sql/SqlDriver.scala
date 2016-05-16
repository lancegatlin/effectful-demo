package effectful.examples.effects.sql

import scala.language.higherKinds

trait SqlDriver[E[_]] {
  import SqlDriver._

  def getConnectionPool(url: String, username: String, password: String) : E[ConnectionPool]
  def closeConnectionPool(connectionPool: ConnectionPool) : E[Unit]

  def beginTransaction()(implicit context: Context.AutoCommit) : E[Context.InTransaction]
  def rollback()(implicit context: Context.InTransaction) : E[Unit]
  def commit()(implicit context: Context.InTransaction) : E[Unit]

  def prepare(statement: String)(implicit context: Context) : E[PreparedStatement]
  def executePreparedQuery(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ) : E[Cursor]

  def executePreparedUpdate(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ) : E[Int]


  def executeQuery(statement: String)(implicit context: Context) : E[Cursor]
  def executeUpdate(statement: String)(implicit context: Context) : E[Int]

  def getCursorMetadata(cursor: Cursor) : E[CursorMetadata]

  def nextCursor(cursor: Cursor) : E[Cursor]

  def closeCursor(cursor: Cursor) : E[Unit]
}

object SqlDriver {
  type SqlRow = IndexedSeq[SqlVal]

  case class ColumnMetadata(
    name: String,
    label: String,
    sqlType: SqlType,
    autoIncrement: Boolean,
    caseSensitive: Boolean,
    nullable: Boolean,
    signed: Boolean
  )

  case class CursorMetadata(
    schemaName: String,
    tableName: String,
    columns : IndexedSeq[ColumnMetadata]
  )

  case class PreparedStatement(
    id: Symbol,
    statement: String
  )

  case class ConnectionPool(
    id: Symbol
  )
  
  sealed trait Context {
    def connectionPool: ConnectionPool
    def isInTransaction: Boolean
  }
  object Context {
    case class AutoCommit(connectionPool: ConnectionPool) extends Context {
      override def isInTransaction = false
    }
    case class InTransaction(id: Symbol, connectionPool: ConnectionPool) extends Context {
      override def isInTransaction = true
    }
  }

  sealed trait Cursor {
    def id: Symbol
    def isEmpty : Boolean
    def nonEmpty:  Boolean = !isEmpty
  }
  object Cursor {
    case class Empty(id: Symbol) extends Cursor {
      override def isEmpty = true
    }
    case class Row(
      id: Symbol,
      rowNum: Int,
      row: SqlRow
    ) extends Cursor {
      override def isEmpty = false
    }
  }

}