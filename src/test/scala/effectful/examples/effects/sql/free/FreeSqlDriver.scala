package effectful.examples.effects.sql.free

import effectful.examples.effects.sql._
import SqlDriver._
import effectful.free.Free

class FreeSqlDriver extends SqlDriver[FreeSqlDriverCmd] {
  import Free._
  import SqlDriverCmd._


  override def beginTransaction(): FreeSqlDriverCmd[Context.InTransaction] =
    Command(BeginTransaction)

  override def commit()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Command(Commit(context))
  
  override def rollback()(implicit context: Context.InTransaction): FreeSqlDriverCmd[Unit] =
    Command(Rollback(context))


  override def prepare(statement: String)(implicit context: Context): FreeSqlDriverCmd[PreparedStatementId] =
    Command(Prepare(statement, context))

  override def executePreparedQuery(preparedStatementId: PreparedStatementId)(rows: SqlRow*): FreeSqlDriverCmd[InitialCursor] =
    Command(ExecutePreparedQuery(preparedStatementId,rows))

  override def executePreparedUpdate(preparedStatementId: PreparedStatementId)(rows: SqlRow*): FreeSqlDriverCmd[Int] =
    Command(ExecutePreparedUpdate(preparedStatementId,rows))


  override def executeQuery(statement: String)(implicit context: Context): FreeSqlDriverCmd[InitialCursor] =
    Command(ExecuteQuery(statement,context))

  override def executeUpdate(statement: String)(implicit context: Context): FreeSqlDriverCmd[Int] =
    Command(ExecuteUpdate(statement,context))


  override def getCursorMetadata(cursorId: CursorId): FreeSqlDriverCmd[CursorMetadata] =
    Command(GetCursorMetadata(cursorId))

  override def nextCursor(cursorId: CursorId): FreeSqlDriverCmd[Cursor] =
    Command(NextCursor(cursorId))

  override def closeCursor(cursorId: CursorId): FreeSqlDriverCmd[Unit] =
    Command(CloseCursor(cursorId))
}
