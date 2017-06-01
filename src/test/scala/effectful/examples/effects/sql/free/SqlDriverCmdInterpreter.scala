package effectful.examples.effects.sql.free

import cats._
import effectful.augments._
import effectful.examples.effects.sql._
import effectful.free.Interpreter

class SqlDriverCmdInterpreter[E[_]](
  sqlDriver: SqlDriver[E]                                   
)(implicit
  val C:Capture[E],
  val M:Monad[E],
  val D:Delay[E],
  val P:Par[E],
  val X:Exceptions[E]
) extends Interpreter[SqlDriverCmd,E] {
  def apply[AA](cmd: SqlDriverCmd[AA]) : E[AA] = {
    import SqlDriverCmd._
    import sqlDriver._

    cmd match {
      case BeginTransaction =>
        beginTransaction()
      case Rollback(context) =>
        rollback()(context)
      case Commit(context) =>
        commit()(context)

      case Prepare(statement,context) =>
        prepare(statement)(context)
      case ExecutePreparedQuery(preparedStatementId, rows) =>
        executePreparedQuery(preparedStatementId)(rows:_*)
      case ExecutePreparedUpdate(preparedStatementId, rows) =>
        executePreparedUpdate(preparedStatementId)(rows:_*)
      case ExecuteQuery(statement,context) =>
        executeQuery(statement)(context)
      case ExecuteUpdate(statement,context) =>
        executeUpdate(statement)(context)

      case GetCursorMetadata(cursorId) =>
        getCursorMetadata(cursorId)
      case NextCursor(cursorId) =>
        nextCursor(cursorId)
      case CloseCursor(cursorId) =>
        closeCursor(cursorId)
    }
  }

}
