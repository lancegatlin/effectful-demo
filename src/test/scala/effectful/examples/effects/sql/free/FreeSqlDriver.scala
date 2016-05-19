package effectful.examples.effects.sql.free

import effectful.Free
import effectful.examples.effects.sql._
import SqlDriver._

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  import Free._
  import SqlDriverCmd._


  override def beginTransaction(): FreeSqlDriverCmd[Context.InTransaction] =
    Cmd(BeginTransaction)

  override def commit()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Cmd(Commit(context))
  
  override def rollback()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Cmd(Rollback(context))


  override def prepare(statement: String)(implicit context: Context): FreeSqlDriverCmd[PreparedStatementId] =
    Cmd(Prepare(statement, context))

  override def executePreparedQuery(preparedStatementId: PreparedStatementId)(rows: SqlRow*): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecutePreparedQuery(preparedStatementId,rows))

  override def executePreparedUpdate(preparedStatementId: PreparedStatementId)(rows: SqlRow*): FreeSqlDriverCmd[Int] =
    Cmd(ExecutePreparedUpdate(preparedStatementId,rows))


  override def executeQuery(statement: String)(implicit context: Context): FreeSqlDriverCmd[Cursor] =
    Cmd(ExecuteQuery(statement,context))

  override def executeUpdate(statement: String)(implicit context: Context): FreeSqlDriverCmd[Int] =
    Cmd(ExecuteUpdate(statement,context))


  override def getCursorMetadata(cursorId: CursorId): FreeSqlDriverCmd[CursorMetadata] =
    Cmd(GetCursorMetadata(cursorId))

  override def nextCursor(cursorId: CursorId): FreeSqlDriverCmd[Cursor] =
    Cmd(NextCursor(cursorId))

  override def closeCursor(cursorId: CursorId): FreeSqlDriverCmd[Unit] =
    Cmd(CloseCursor(cursorId))
}
