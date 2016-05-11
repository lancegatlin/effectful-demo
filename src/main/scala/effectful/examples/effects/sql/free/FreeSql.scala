package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._

class FreeSql extends Sql[FreeSqlCmd] {
  override def executePreparedUpdate(preparedStatement: PreparedStatement, args: Seq[SqlVal]*): FreeSqlCmd[Int] =
    Free.Cmd(SqlCmd.ExecutePreparedUpdate(preparedStatement,args))

  override def executePreparedQuery(preparedStatement: PreparedStatement, args: Seq[SqlVal]*): FreeSqlCmd[Cursor] =
    Free.Cmd(SqlCmd.ExecutePreparedQuery(preparedStatement,args))

  override def executeQuery(connection: Connection, statement: String): FreeSqlCmd[Cursor] =
    Free.Cmd(SqlCmd.ExecuteQuery(connection, statement))

  override def executeUpdate(connection: Connection, statement: String): FreeSqlCmd[Int] =
    Free.Cmd(SqlCmd.ExecuteUpdate(connection,statement))

  override def getConnection(url: String, username: String, password: String): FreeSqlCmd[Connection] =
    Free.Cmd(SqlCmd.GetConnection(url,username,password))

  override def prepare(connection: Connection, statement: String): FreeSqlCmd[PreparedStatement] =
    Free.Cmd(SqlCmd.Prepare(connection, statement))
}
