package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._
import SqlDriver._


sealed trait SqlDriverCmd[R]

object SqlDriverCmd {
  case class GetConnectionPool(
    url: String,
    username: String,
    password: String
  ) extends SqlDriverCmd[ConnectionPool]

  case class CloseConnectionPool(
    connection: ConnectionPool
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
    rows: Seq[SqlRow]
  ) extends SqlDriverCmd[Cursor]

  case class ExecutePreparedUpdate(
    preparedStatement: PreparedStatement,
    rows: Seq[SqlRow]
  ) extends SqlDriverCmd[Int]


  case class ExecuteQuery(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[Cursor]

  case class ExecuteUpdate(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[Int]


  case class GetCursorMetadata(
    cursor: Cursor
  ) extends SqlDriverCmd[CursorMetadata]

  case class NextCursor(
    cursor: Cursor
  ) extends SqlDriverCmd[Cursor]

  case class CloseCursor(
    cursor: Cursor
  ) extends SqlDriverCmd[Unit]

}
