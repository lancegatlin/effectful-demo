package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  override def executePreparedUpdate(preparedStatement: PreparedStatement)(args: Seq[SqlVal]*)(implicit connection: Connection): FreeSqlDriverCmd[Int] =
    Free.Cmd(SqlDriverCmd.ExecutePreparedUpdate(preparedStatement,args))

  override def executePreparedQuery(preparedStatement: PreparedStatement)(args: Seq[SqlVal]*)(implicit connection: Connection): FreeSqlDriverCmd[Cursor] =
    Free.Cmd(SqlDriverCmd.ExecutePreparedQuery(preparedStatement,args))

  override def executeQuery(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[Cursor] =
    Free.Cmd(SqlDriverCmd.ExecuteQuery(statement))

  override def executeUpdate(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[Int] =
    Free.Cmd(SqlDriverCmd.ExecuteUpdate(statement))

  override def getConnection(url: String, username: String, password: String): FreeSqlDriverCmd[Connection] =
    Free.Cmd(SqlDriverCmd.GetConnection(url,username,password))

  override def prepare(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[PreparedStatement] =
    Free.Cmd(SqlDriverCmd.Prepare(statement))

  override def executeTransaction[A](f: FreeSqlDriver => FreeSqlDriverCmd[A]): FreeSqlDriverCmd[A] = f(this)
}
