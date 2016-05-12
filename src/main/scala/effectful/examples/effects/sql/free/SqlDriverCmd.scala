package effectful.examples.effects.sql.free

import effectful.examples.effects.sql.{Connection, _}


sealed trait SqlDriverCmd[R]

object SqlDriverCmd {
  case class GetConnection(
    url: String,
    username: String,
    password: String
  ) extends SqlDriverCmd[Connection]

  case class Prepare(
    statement: String
  )(implicit
    val connection: Connection
  ) extends SqlDriverCmd[PreparedStatement]

  case class ExecutePreparedQuery(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  )(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Cursor]

  case class ExecutePreparedUpdate(
    preparedStatement: PreparedStatement,
    args: Seq[Seq[SqlVal]]
  )(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Int]

  case class ExecuteQuery(
    statement: String
  )(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Cursor]

  case class ExecuteUpdate(
    statement: String
  )(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Int]
  
  case class BeginTransaction()(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Unit]
  
  case class Commit()(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Unit]
  
  case class Rollback()(implicit
    val connection: Connection
  ) extends SqlDriverCmd[Unit]

  case class Close(
    connection: Connection
  ) extends SqlDriverCmd[Unit]
}
