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
      case GetConnection(
        url,
        username,
        password
      ) =>
        getConnection(
          url = url,
          username = username,
          password = password
        )
      case CloseConnection(connection) =>
        closeConnection(connection)

      case BeginTransaction(context) =>
        beginTransaction()(context)
      case Rollback(context) =>
        rollback()(context)
      case Commit(context) =>
        commit()(context)

      case Prepare(statement,context) =>
        prepare(statement)(context)
      case ExecutePreparedQuery(preparedStatement, rows, context) =>
        executePreparedQuery(preparedStatement)(rows:_*)(context)
      case ExecutePreparedUpdate(preparedStatement, rows, context) =>
        executePreparedUpdate(preparedStatement)(rows:_*)(context)
      case ExecuteQuery(statement,context) =>
        executeQuery(statement)(context)
      case ExecuteUpdate(statement,context) =>
        executeUpdate(statement)(context)

      case GetMetadata(cursor) =>
        getMetadata(cursor)
      case SeekAbsolute(cursor, rowNum) =>
        seekAbsolute(cursor, rowNum)
      case SeekRelative(cursor, rowOffset) =>
        seekRelative(cursor, rowOffset)
      case SeekFirst(cursor) =>
        seekFirst(cursor)
      case SeekLast(cursor) =>
        seekLast(cursor)
      case SetSeekDir(cursor, forward) =>
        setSeekDir(cursor, forward)
      case NextRow(cursor) =>
        nextRow(cursor)
      case CloseCursor(cursor) =>
        closeCursor(cursor)
    }
  }

}
