package effectful.examples.effects.sql.free

import effectful.{EffectSystem, Free}

import scala.language.higherKinds
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
      case GetConnectionPool(
        url,
        username,
        password
      ) =>
        getConnectionPool(
          url = url,
          username = username,
          password = password
        )
      case CloseConnectionPool(connection) =>
        closeConnectionPool(connection)

      case BeginTransaction(context) =>
        beginTransaction()(context)
      case Rollback(context) =>
        rollback()(context)
      case Commit(context) =>
        commit()(context)

      case Prepare(statement,context) =>
        prepare(statement)(context)
      case ExecutePreparedQuery(preparedStatement, rows) =>
        executePreparedQuery(preparedStatement)(rows:_*)
      case ExecutePreparedUpdate(preparedStatement, rows) =>
        executePreparedUpdate(preparedStatement)(rows:_*)
      case ExecuteQuery(statement,context) =>
        executeQuery(statement)(context)
      case ExecuteUpdate(statement,context) =>
        executeUpdate(statement)(context)

      case GetCursorMetadata(cursor) =>
        getCursorMetadata(cursor)
      case NextCursor(cursor) =>
        nextCursor(cursor)
      case CloseCursor(cursor) =>
        closeCursor(cursor)
    }
  }

}
