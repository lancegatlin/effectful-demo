package effectful.examples.pure.dao.sql

import effectful.examples.effects.sql._
import effectful.examples.effects.sql.SqlDriver.SqlRow
import effectful.examples.pure.dao.DocDao.RecordMetadata

trait SqlValFormat[A] {
  def toSqlVal(a: A) : SqlVal
  def fromSqlVal(v: SqlVal) : A
}

trait SqlRowFormat[A] {
  def toSqlRow(a: A) : SqlRow
  def fromSqlRow(row: SqlRow) : A
}

trait SqlRecordFormat[ID,A] extends SqlValFormat[ID] with SqlRowFormat[A]

object SqlRecordFormat {
  def apply[ID,A](
    idFormat: SqlValFormat[ID],
    rowFormat: SqlRowFormat[RecordMetadata]
  ) : SqlRecordFormat[ID,RecordMetadata] =
    new SqlRecordFormat[ID,RecordMetadata] {
      def toSqlVal(a: ID) = idFormat.toSqlVal(a)
      def fromSqlVal(v: SqlVal) = idFormat.fromSqlVal(v)
      def fromSqlRow(row: SqlRow) = rowFormat.fromSqlRow(row)
      def toSqlRow(a: RecordMetadata) = rowFormat.toSqlRow(a)
    }
}

trait CharDataFormat[A] {
  def toCharData(a: A) : CharData
  def fromCharData(data: CharData) : A
}

object CharDataFormat {
  implicit val charDataFormat_String = new CharDataFormat[String] {
    def toCharData(a: String) = CharData(a)
    def fromCharData(data: CharData) = data.toCharString()
  }
  implicit val charDataFormat_Reader = new CharDataFormat[java.io.Reader] {
    def toCharData(a: java.io.Reader) = CharData(a)
    def fromCharData(data: CharData) = data.toCharStream()
  }
}

trait BinDataFormat[A] {
  def toBinData(a: A) : BinData
  def fromBinData(data: BinData) : A
}