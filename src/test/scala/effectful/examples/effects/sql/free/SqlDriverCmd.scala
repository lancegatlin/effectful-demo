package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._
import SqlDriver._


sealed trait SqlDriverCmd[R]

object SqlDriverCmd {
  case object BeginTransaction extends SqlDriverCmd[Context.InTransaction]

  case class Commit(
    context: Context.InTransaction
  ) extends SqlDriverCmd[Unit]

  case class Rollback(
    context: Context.InTransaction
  ) extends SqlDriverCmd[Unit]


  case class Prepare(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[PreparedStatementId]

  case class ExecutePreparedQuery(
    preparedStatementId: PreparedStatementId,
    rows: Seq[SqlRow]
  ) extends SqlDriverCmd[InitialCursor]

  case class ExecutePreparedUpdate(
    preparedStatementId: PreparedStatementId,
    rows: Seq[SqlRow]
  ) extends SqlDriverCmd[Int]


  case class ExecuteQuery(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[InitialCursor]

  case class ExecuteUpdate(
    statement: String,
    context: Context
  ) extends SqlDriverCmd[Int]


  case class GetCursorMetadata(
    cursorId: CursorId
  ) extends SqlDriverCmd[CursorMetadata]

  case class NextCursor(
    cursorId: CursorId
  ) extends SqlDriverCmd[Cursor]

  case class CloseCursor(
    cursorId: CursorId
  ) extends SqlDriverCmd[Unit]

}
