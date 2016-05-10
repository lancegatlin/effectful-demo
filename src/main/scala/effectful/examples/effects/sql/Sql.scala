package effectful.examples.effects.sql

import scala.language.higherKinds
import org.apache.commons.io.IOUtils

trait Sql[E[_]] {
  import Sql._

  def getConnection(url: String, username: String, password: String) : E[Connection]

  def prepare(connection: Connection, statement: String) : E[PreparedStatement]
  def executePreparedQuery(preparedStatement: PreparedStatement, batches: Seq[SqlVal]*) : E[Cursor]
  def executePreparedUpdate(preparedStatement: PreparedStatement, args: Seq[SqlVal]*) : E[Int]

  def executeQuery(connection: Connection, statement: String) : E[Cursor]
  def executeUpdate(connection: Connection, statement: String) : E[Int]
}

object Sql {
  trait Connection {
    def url: String

    def close() : Unit
    def commit() : Unit
  }
  trait PreparedStatement {
    def statement: String
  }
  type Row = Int => SqlVal

  trait Cursor extends Iterator[Row] {
    def schemaName: String
    def tableName: String

    def columnCount : Int

    def columns(i: Int) : ColumnMetadata

    def seekAbsolute(rowNum: Int) : Row
    def seekRelative(rowOffset: Int) : Row

    def isBeforeFirst : Boolean
    def isFirst : Boolean

    def first() : Unit
    def last() : Unit
    def currentRowNum : Int

    def reverse() : Unit

    def isClosed : Boolean
    def close() : Unit
  }

  case class ColumnMetadata(
    name: String,
    label: String,
    sqlType: SqlType,
    autoIncrement: Boolean,
    caseSensitive: Boolean,
    nullable: Boolean,
    signed: Boolean
  )
  sealed trait SqlType
  object SqlType {
    case class CHAR(fixedSize: Long) extends SqlType
    type CHARACTER = CHAR
    val CHARACTER = CHAR
    case class NCHAR(fixedSize: Long) extends SqlType // unicode
    case class VARCHAR(
      maxLength: Long
    ) extends SqlType
    case class NVARCHAR(
      maxLength: Long
    ) extends SqlType // unicode
    case object CLOB extends SqlType
    case object NCLOB extends SqlType
    case class BINARY(fixedSize: Long) extends SqlType
    case class VARBINARY(maxSize: Long) extends SqlType
    case class BLOB() extends SqlType
    case object BIT extends SqlType
    case object BOOLEAN extends SqlType
    case class INTEGERP(precision: Int) extends SqlType
    case object TINYINT extends SqlType
    case object SMALLINT extends SqlType // precision = 5
    case object INTEGER extends SqlType // precision = 10
    case object BIGINT extends SqlType // precision = 19

  }

  sealed trait SqlVal {
    def sqlType: SqlType
  }
  object SqlVal {
    case object NULL extends SqlType
    case class CHARACTER(
      fixedLength: Int
    )(
      toCharStream: () => java.io.Reader,
      toCharString: () => String
    ) extends SqlVal {
      val sqlType = SqlType.CHARACTER(fixedLength)
    }
    object CHARACTER {
      def apply(value: String) : CHARACTER = CHARACTER(0)(
        toCharStream = () => new java.io.StringReader(value),
        toCharString = () => value
      )
    }
    case class VARCHAR(
      maxLength: Int
    )(
      toCharStream: () => java.io.Reader,
      toCharString: () => String
    ) extends SqlVal {
      val sqlType = SqlType.VARCHAR(maxLength)
    }
    object VARCHAR {
      def apply(value: String) : VARCHAR = VARCHAR(0)(
        toCharStream = () => new java.io.StringReader(value),
        toCharString = () => value
      )
    }
    case class BINARY(
      fixedSize: Int
    )(
      toBinaryStream: () => java.io.InputStream,
      toByteArray: () => Array[Byte]
    ) extends SqlVal {
      val sqlType = SqlType.BINARY(fixedSize)
    }
    object BINARY {
      def apply(bin: java.io.InputStream) : BINARY = BINARY(0)(
        toBinaryStream = () => bin,
        toByteArray = () =>IOUtils.toByteArray(bin)
      )
    }
    case class VARBINARY(
      maxSize: Int
    )(
      toBinaryStream: () => java.io.InputStream,
      toByteArray: () => Array[Byte]
    ) extends SqlVal {
      val sqlType = SqlType.VARBINARY(maxSize)
    }
    object VARBINARY {
      def apply(bin: java.io.InputStream) : BINARY = BINARY(0)(
        toBinaryStream = () => bin,
        toByteArray = () => IOUtils.toByteArray(bin)
      )
    }
    case class BOOLEAN(value: Boolean) extends SqlVal {
      val sqlType = SqlType.BOOLEAN
    }

  }
}
