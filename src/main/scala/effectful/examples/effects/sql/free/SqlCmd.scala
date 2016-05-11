package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._


sealed trait SqlCmd[R]

object SqlCmd {
  case class GetConnection(
    url: String,
    username: String, 
    password: String
  ) extends SqlCmd[Connection]

  case class Prepare(
    connection: Connection,
    statement: String
  ) extends SqlCmd[PreparedStatement]

  case class ExecutePreparedQuery(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  ) extends SqlCmd[Cursor]

  case class ExecutePreparedUpdate(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  ) extends SqlCmd[Int]

  case class ExecuteQuery(
    connection: Connection,
    statement: String
  ) extends SqlCmd[Cursor]
  case class ExecuteUpdate(
    connection: Connection,
    statement: String
  ) extends SqlCmd[Int]
}
