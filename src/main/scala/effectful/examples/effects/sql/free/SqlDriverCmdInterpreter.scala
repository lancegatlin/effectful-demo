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
      case c@Prepare(statement) =>
        prepare(statement)(c.connection)
      case c@ExecutePreparedQuery(preparedStatement, args) =>
        executePreparedQuery(preparedStatement)(args:_*)(c.connection)
      case c@ExecutePreparedUpdate(preparedStatement, args) =>
        executePreparedUpdate(preparedStatement)(args:_*)(c.connection)
      case c@ExecuteQuery(statement) =>
        executeQuery(statement)(c.connection)
      case c@ExecuteUpdate(statement) =>
        executeUpdate(statement)(c.connection)
      case c@BeginTransaction() =>
        beginTransaction()(c.connection)
      case c@Rollback() =>
        rollback()(c.connection)
      case c@Commit() =>
        commit()(c.connection)
      case c@Close(connection) =>
        close()(connection)
    }
  }

}
