package effectful.examples.effects.sql.free

import effectful.{EffectSystem, Free}

import effectful.examples.effects.sql._

class SqlDriverCmdInterpreter[E[_]](
  sqlDriver: SqlDriver[E]                                   
)(implicit
  val E:EffectSystem[E]
) extends Free.Interpreter[SqlDriverCmd,E] {
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
