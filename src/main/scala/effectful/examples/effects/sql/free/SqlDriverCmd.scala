package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._


sealed trait SqlDriverCmd[R]

object SqlDriverCmd {
  case class GetConnection(
    url: String,
    username: String,
    password: String
  ) extends SqlDriverCmd[Connection]

  case class Prepare(
    connection: Connection,
    statement: String
  ) extends SqlDriverCmd[PreparedStatement]

  case class ExecutePreparedQuery(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  ) extends SqlDriverCmd[Cursor]

  case class ExecutePreparedUpdate(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  ) extends SqlDriverCmd[Int]

  case class ExecuteQuery(
    connection: Connection,
    statement: String
  ) extends SqlDriverCmd[Cursor]
  case class ExecuteUpdate(
    connection: Connection,
    statement: String
  ) extends SqlDriverCmd[Int]
}
