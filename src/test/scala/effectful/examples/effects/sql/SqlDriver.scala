package effectful.examples.effects.sql

import effectful._
import effectful.examples.effects.sql.SqlDriver.Context.InTransaction

trait SqlDriver[E[_]] {
  import SqlDriver._

  def beginTransaction() : E[Context.InTransaction]
  def rollback()(implicit context: Context.InTransaction) : E[Unit]
  def commit()(implicit context: Context.InTransaction) : E[Unit]

  def prepare(statement: String)(implicit context: Context) : E[PreparedStatementId]
  def executePreparedQuery(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ): E[Cursor]

  def executePreparedUpdate(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ) : E[Int]


  def executeQuery(statement: String)(implicit context: Context) : E[Cursor]
  def executeUpdate(statement: String)(implicit context: Context) : E[Int]

  def getCursorMetadata(cursorId: CursorId) : E[CursorMetadata]

  def nextCursor(cursorId: CursorId) : E[Cursor]

  def closeCursor(cursorId: CursorId) : E[Unit]

  //todo: add input stream reading calls here? maybe spin off StreamService?
  // case class Chunk(bytes: Array[Byte], index: Int)
  // case class ReadStream(chunkSize: Int, totalSize: Option[Int], bytesReadSoFar: Int)
  // case class WriteStream(maxChunkSize: Int, bytesWrittenSoFar: Int)
  // def getStreamInfo(streamId: StreamId) : E[Stream]
  // def readStreamChunk(streamId: StreamId, index: Int) : E[Chunk]
  // def readNextStreamChunk(streamId: StreamId) : E[Option[Chunk]]
  // def writeStreamChunk(streamId: StreamId, chunk: Chunk) : E[Boolean]
  // def closeStream(streamId: StreamId) : E[Unit]
}

object SqlDriver {
  type SqlRow = IndexedSeq[SqlVal]

  case class ColumnMetadata(
    name: String,
    label: String,
    sqlType: SqlType,
    autoIncrement: Boolean,
    caseSensitive: Boolean,
    nullable: Boolean,
    signed: Boolean
  )

  case class CursorMetadata(
    schemaName: String,
    tableName: String,
    columns : IndexedSeq[ColumnMetadata]
  )

  type PreparedStatementId = Symbol

  sealed trait Context {
    def isInTransaction: Boolean
  }
  object Context {
    case object AutoCommit extends Context {
      override def isInTransaction = false
    }
    case class InTransaction(id: Symbol) extends Context {
      override def isInTransaction = true
    }
  }

  type CursorId = Symbol

  sealed trait Cursor {
    def id: CursorId
    def isEmpty : Boolean
    def nonEmpty:  Boolean = !isEmpty
  }
  object Cursor {
    case class Empty(id: CursorId) extends Cursor {
      override def isEmpty = true
    }
    case class Row(
      id: CursorId,
      rowNum: Int,
      row: SqlRow
    ) extends Cursor {
      override def isEmpty = false
    }
  }

  implicit object LiftS_SqlDriver extends LiftS[SqlDriver] {
    override def apply[E[_], F[_]](
      s: SqlDriver[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): SqlDriver[F] =
      new SqlDriver[F] {
        override def beginTransaction(): F[InTransaction] =
          liftE(s.beginTransaction())
        override def executePreparedUpdate(preparedStatementId: PreparedStatementId)(rows: SqlRow*): F[Int] =
          liftE(s.executePreparedUpdate(preparedStatementId)(rows:_*))
        override def nextCursor(cursorId: CursorId): F[Cursor] =
          liftE(s.nextCursor(cursorId))
        override def executePreparedQuery(preparedStatementId: PreparedStatementId)(rows: SqlRow*): F[Cursor] =
          liftE(s.executePreparedQuery(preparedStatementId)(rows:_*))
        override def getCursorMetadata(cursorId: CursorId): F[CursorMetadata] =
          liftE(s.getCursorMetadata(cursorId))
        override def closeCursor(cursorId: CursorId): F[Unit] =
          liftE(s.closeCursor(cursorId))
        override def rollback()(implicit context: InTransaction): F[Unit] =
          liftE(s.rollback())
        override def executeQuery(statement: String)(implicit context: Context): F[Cursor] =
          liftE(s.executeQuery(statement))
        override def executeUpdate(statement: String)(implicit context: Context): F[Int] =
          liftE(s.executeUpdate(statement))
        override def prepare(statement: String)(implicit context: Context): F[PreparedStatementId] =
          liftE(s.prepare(statement))
        override def commit()(implicit context: InTransaction): F[Unit] =
          liftE(s.commit())
      }
  }  
}