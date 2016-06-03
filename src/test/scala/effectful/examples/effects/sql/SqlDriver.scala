package effectful.examples.effects.sql

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
  ): E[InitialCursor]

  def executePreparedUpdate(
    preparedStatementId: PreparedStatementId
  )(
    rows: SqlRow*
  ) : E[Int]

  def executeQuery(statement: String)(implicit context: Context) : E[InitialCursor]
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

  sealed trait InitialCursor {
    def isEmpty : Boolean
    def nonEmpty:  Boolean = !isEmpty
  }
  object InitialCursor {
    case class NonEmpty(id: CursorId) extends InitialCursor {
      override def isEmpty = false
    }
    case object Empty extends InitialCursor {
      override def isEmpty = true
    }
  }

  sealed trait Cursor {
    def isEmpty : Boolean
    def nonEmpty:  Boolean = !isEmpty
  }
  object Cursor {
    case object Empty extends Cursor {
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
}