package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._
import SqlDriver._

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  import Free._
  import SqlDriverCmd._


  override def getConnection(url: String, username: String, password: String): FreeSqlDriverCmd[Connection] =
    Cmd(GetConnection(url,username,password))

  override def closeConnection(connection: Connection): FreeSqlDriverCmd[Unit] =
    Cmd(CloseConnection(connection))
  
  
  override def beginTransaction()(implicit context: Context.AutoCommit): FreeSqlDriverCmd[Context.InTransaction] =
    Cmd(BeginTransaction(context))

  override def commit()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Cmd(Commit(context))
  
  override def rollback()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Cmd(Rollback(context))


  override def prepare(statement: String)(implicit context: Context): FreeSqlDriverCmd[PreparedStatement] =
    Cmd(Prepare(statement, context))

  override def executePreparedQuery(preparedStatement: PreparedStatement)(rows: SqlRow*)(implicit context: Context): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecutePreparedQuery(preparedStatement,rows,context))

  override def executePreparedUpdate(preparedStatement: PreparedStatement)(rows: SqlRow*)(implicit context: Context): FreeSqlDriverCmd[Int] =
    Cmd(ExecutePreparedUpdate(preparedStatement,rows,context))


  override def executeQuery(statement: String)(implicit context: Context): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecuteQuery(statement,context))

  override def executeUpdate(statement: String)(implicit context: Context): FreeSqlDriverCmd[Int] =
    Cmd(ExecuteUpdate(statement,context))


  override def getMetadata(cursor: Cursor): FreeSqlDriverCmd[CursorMetadata] =
    Cmd(GetMetadata(cursor))

  override def seekAbsolute(cursor: Cursor, rowNum: Int): FreeSqlDriverCmd[Cursor] =
    Cmd(SeekAbsolute(cursor, rowNum))

  override def seekLast(cursor: Cursor): FreeSqlDriverCmd[Cursor] =
    Cmd(SeekLast(cursor))

  override def nextRow(cursor: Cursor): FreeSqlDriverCmd[Cursor] =
    Cmd(NextRow(cursor))

  override def seekRelative(cursor: Cursor, rowOffset: Int): FreeSqlDriverCmd[Cursor] =
    Cmd(SeekRelative(cursor, rowOffset))

  override def setSeekDir(cursor: Cursor, forward: Boolean): FreeSqlDriverCmd[Unit] =
    Cmd(SetSeekDir(cursor, forward))

  override def seekFirst(cursor: Cursor): FreeSqlDriverCmd[Cursor] =
    Cmd(SeekFirst(cursor))

  override def closeCursor(cursor: Cursor): FreeSqlDriverCmd[Unit] =
    Cmd(CloseCursor(cursor))
}
