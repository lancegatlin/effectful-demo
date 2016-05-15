package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._
import SqlDriver._

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  import Free._
  import SqlDriverCmd._


  override def getConnectionPool(url: String, username: String, password: String): FreeSqlDriverCmd[ConnectionPool] =
    Cmd(GetConnectionPool(url,username,password))

  override def closeConnectionPool(connection: ConnectionPool): FreeSqlDriverCmd[Unit] =
    Cmd(CloseConnectionPool(connection))
  
  
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


  override def getCursorMetadata(cursor: Cursor): FreeSqlDriverCmd[CursorMetadata] =
    Cmd(GetCursorMetadata(cursor))

  override def nextCursor(cursor: Cursor): FreeSqlDriverCmd[Cursor] =
    Cmd(NextCursor(cursor))

  override def closeCursor(cursor: Cursor): FreeSqlDriverCmd[Unit] =
    Cmd(CloseCursor(cursor))
}
