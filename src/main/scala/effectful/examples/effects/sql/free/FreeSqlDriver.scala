package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  import Free._
  import SqlDriverCmd._
  
  override def executePreparedUpdate(preparedStatement: PreparedStatement)(args: Seq[SqlVal]*)(implicit connection: Connection): FreeSqlDriverCmd[Int] =
    Cmd(ExecutePreparedUpdate(preparedStatement,args))

  override def executePreparedQuery(preparedStatement: PreparedStatement)(args: Seq[SqlVal]*)(implicit connection: Connection): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecutePreparedQuery(preparedStatement,args))

  override def executeQuery(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecuteQuery(statement))

  override def executeUpdate(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[Int] =
    Cmd(ExecuteUpdate(statement))

  override def getConnection(url: String, username: String, password: String): FreeSqlDriverCmd[Connection] =
    Cmd(GetConnection(url,username,password))

  override def prepare(statement: String)(implicit connection: Connection): FreeSqlDriverCmd[PreparedStatement] =
    Cmd(Prepare(statement))

  override def beginTransaction()(implicit connection: Connection): FreeSqlDriverCmd[Unit] =
    Cmd(BeginTransaction())

  override def commit()(implicit connection: Connection): FreeSqlDriverCmd[Unit] =
    Cmd(Commit())
  
  override def rollback()(implicit connection: Connection): FreeSqlDriverCmd[Unit] =
    Cmd(Rollback())

  override def close()(connection: Connection): FreeSqlDriverCmd[Unit] =
    Cmd(Close(connection))
}
