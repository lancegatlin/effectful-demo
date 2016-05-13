package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._
import SqlDriver._


sealed trait SqlDriverCmd[R]

object SqlDriverCmd {
  case class GetConnection(
    url: String,
    username: String,
    password: String
  ) extends SqlDriverCmd[Connection]

  case class CloseConnection(
    connection: Connection
  ) extends SqlDriverCmd[Unit]


  case class BeginTransaction(
    context: Context.AutoCommit
  ) extends SqlDriverCmd[Context.InTransaction]

  case class Commit(
    context: Context.InTransaction
  ) extends SqlDriverCmd[Unit]

  case class Rollback(
    context: Context.InTransaction
  ) extends SqlDriverCmd[Unit]


  case class Prepare(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[PreparedStatement]

  case class ExecutePreparedQuery(
    preparedStatement: PreparedStatement,
    rows: Seq[SqlRow],
    context: Context
  ) extends SqlDriverCmd[Cursor]

  case class ExecutePreparedUpdate(
    preparedStatement: PreparedStatement,
    rows: Seq[SqlRow],
    context: Context
  ) extends SqlDriverCmd[Int]


  case class ExecuteQuery(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[Cursor]

  case class ExecuteUpdate(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[Int]


  case class GetMetadata(
    cursor: Cursor
  ) extends SqlDriverCmd[CursorMetadata]

  case class SeekAbsolute(
    cursor: Cursor,
    rowNum: Int
  ) extends SqlDriverCmd[Cursor]

  case class SeekRelative(
    cursor: Cursor,
    rowOffset: Int
  ) extends SqlDriverCmd[Cursor]

  case class SeekFirst(
    cursor: Cursor
  ) extends SqlDriverCmd[Cursor]

  case class SeekLast(
    cursor: Cursor
  ) extends SqlDriverCmd[Cursor]

  case class SetSeekDir(
    cursor: Cursor,
    forward: Boolean
  ) extends SqlDriverCmd[Cursor]

  case class NextRow(
    cursor: Cursor
  ) extends SqlDriverCmd[Option[Cursor]]

  case class CloseCursor(
    cursor: Cursor
  ) extends SqlDriverCmd[Unit]

}
