package effectful.examples.effects.sql

import scala.language.higherKinds

trait SqlDriver[E[_]] {
  import SqlDriver._

  def beginTransaction() : E[Context.InTransaction]
  def rollback()(implicit context: Context.InTransaction) : E[Unit]
  def commit()(implicit context: Context.InTransaction) : E[Unit]

  def prepare(statement: String)(implicit context: Context) : E[PreparedStatementId]
  def executePreparedQuery(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ): E[Cursor]

  def executePreparedUpdate(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ) : E[Int]


  def executeQuery(statement: String)(implicit context: Context) : E[Cursor]
  def executeUpdate(statement: String)(implicit context: Context) : E[Int]

  def getCursorMetadata(cursorId: CursorId) : E[CursorMetadata]

  def nextCursor(cursorId: CursorId) : E[Cursor]

  def closeCursor(cursorId: CursorId) : E[Unit]
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

  type PreparedStatementId = Symbol

  sealed trait Context {
    def isInTransaction: Boolean
  }
  object Context {
    case object AutoCommit extends Context {
      override def isInTransaction = false
    }
    case class InTransaction(id: Symbol) extends Context {
      override def isInTransaction = true
    }
  }

  type CursorId = Symbol

  sealed trait Cursor {
    def id: CursorId
    def isEmpty : Boolean
    def nonEmpty:  Boolean = !isEmpty
  }
  object Cursor {
    case class Empty(id: CursorId) extends Cursor {
      override def isEmpty = true
    }
    case class Row(
      id: CursorId,
      rowNum: Int,
      row: SqlRow
    ) extends Cursor {
      override def isEmpty = false
    }
  }

}