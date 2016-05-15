package effectful.examples.effects.sql

import scala.language.higherKinds

trait SqlDriver[E[_]] {
  import SqlDriver._

  def getConnection(url: String, username: String, password: String) : E[Connection]
  def closeConnection(connection: Connection) : E[Unit]

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


  def getMetadata(cursor: Cursor) : E[CursorMetadata]

  def seekAbsolute(cursor: Cursor, rowNum: Int) : E[Cursor]
  def seekRelative(cursor: Cursor, rowOffset: Int) : E[Cursor]

  def seekFirst(cursor: Cursor) : E[Cursor]
  def seekLast(cursor: Cursor) : E[Cursor]
  def setSeekDir(cursor: Cursor, forward: Boolean) : E[Unit]

  def nextRow(cursor: Cursor) : E[Cursor]

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

  // todo: make these case classes with id
  trait PreparedStatement {
    def statement: String
  }
  
  trait Connection {
    def url: String

    def isClosed : Boolean
  }
  
  trait Transaction {
    def isUncommitted: Boolean
  }
  
  sealed trait Context {
    def connection: Connection
  }
  object Context {
    case class AutoCommit(connection: Connection) extends Context
    case class InTransaction(transaction: Transaction, connection: Connection) extends Context
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