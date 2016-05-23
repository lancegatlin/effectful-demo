package effectful.examples.effects.sql

import org.apache.commons.io.IOUtils

sealed trait CharData {
  //todo: can't use Reader -- refactor this to work through EffectIterator w/SqlDriver.readStreamChunk
  def toCharStream() : java.io.Reader
  def toCharString() : String
}
object CharData {
  def apply(reader: java.io.Reader) : IsReader = IsReader(reader)
  def apply(data: String) : IsString = IsString(data)
  
  case class IsReader(reader: java.io.Reader) extends CharData {
    override def toCharStream() = reader
    override def toCharString() = IOUtils.toString(reader)
  }
  case class IsString(data: String) extends CharData {
    override def toCharStream() = new java.io.StringReader(data)
    override def toCharString() = data
  }
}

sealed trait BinData {
  //todo: can't use InputStream -- refactor this to work through EffectIterator w/SqlDriver.readStreamChunk
  def toBinStream() : java.io.InputStream
  def toByteArray() : Array[Byte]
}
object BinData {
  def apply(bin: java.io.InputStream) : IsBinStream = IsBinStream(bin)
  def apply(data: Array[Byte]) : IsByteArray = IsByteArray(data)
  
  case class IsBinStream(bin: java.io.InputStream) extends BinData {
    override def toBinStream() = bin
    override def toByteArray() = IOUtils.toByteArray(bin)
  }
  case class IsByteArray(data: Array[Byte]) extends BinData {
    override def toBinStream() = new java.io.ByteArrayInputStream(data)
    override def toByteArray() = data
  }
}

sealed trait SqlVal {
  def sqlType: SqlType
}
object SqlVal {
  // Mappings based on: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html

  // Note: sqlType is required by JDBC PreparedStatement.setNull
  case class NULL(sqlType: SqlType) extends SqlVal
  case class CHAR(
    fixedLength: Long,
    data: CharData
  ) extends SqlVal {
    def sqlType = SqlType.CHAR(fixedLength)
  }
  case class NCHAR(
    fixedLength: Long,
    data: CharData
  ) extends SqlVal {
    def sqlType = SqlType.NCHAR(fixedLength)
  }
  case class VARCHAR(
    maxLength: Long,
    data: CharData
  ) extends SqlVal {
    def sqlType = SqlType.VARCHAR(maxLength)
  }
  case class NVARCHAR(
    maxLength: Long,
    data: CharData
  ) extends SqlVal {
    def sqlType = SqlType.NVARCHAR(maxLength)
  }
  case class CLOB(data: CharData) extends SqlVal {
    def sqlType = SqlType.CLOB
  }
  case class NCLOB(data: CharData) extends SqlVal {
    def sqlType = SqlType.NCLOB
  }
  case class BINARY(
    fixedSize: Long,
    data: BinData
  ) extends SqlVal {
    def sqlType = SqlType.BINARY(fixedSize)
  }
  case class VARBINARY(
    maxSize: Long,
    data: BinData
  ) extends SqlVal {
    def sqlType = SqlType.VARBINARY(maxSize)
  }
  case class BLOB(data: BinData) extends SqlVal {
    def sqlType = SqlType.BLOB
  }
  case class BOOLEAN(value: Boolean) extends SqlVal {
    def sqlType = SqlType.BOOLEAN
  }
  case class BIT(value: Boolean) extends SqlVal {
    def sqlType = SqlType.BIT
  }
  case class TINYINT(value: Short) extends SqlVal { // +/- 0-255
    def sqlType = SqlType.TINYINT
  }

  case class SMALLINT(value: Short) extends SqlVal {
    def sqlType = SqlType.SMALLINT
  }

  case class INTEGER(value: Int) extends SqlVal {
    def sqlType = SqlType.INTEGER
  }

  case class BIGINT(value: Long) extends SqlVal {
    def sqlType = SqlType.BIGINT
  }

  case class REAL(value: Float) extends SqlVal {
    def sqlType = SqlType.REAL
  }
  case class DOUBLE(value: Double) extends SqlVal {
    def sqlType = SqlType.DOUBLE
  }
  case class NUMERIC(
    value: BigDecimal,
    precision: Int = 0,
    scale: Int = 0
  ) extends SqlVal  {
    def sqlType = SqlType.NUMERIC(precision,scale)
  }
  case class DECIMAL(
    value: BigDecimal,
    precision: Int = 0,
    scale: Int = 0
  ) extends SqlVal {
    def sqlType = SqlType.DECIMAL(precision,scale)
  }
  case class DATE(date: java.time.LocalDate) extends SqlVal {
    def sqlType = SqlType.DATE
  }
  case class TIME(time: java.time.LocalTime) extends SqlVal {
    def sqlType = SqlType.TIME
  }
  case class TIMESTAMP(timestamp: java.time.Instant) extends SqlVal {
    def sqlType = SqlType.TIMESTAMP
  }

  /*
    case NULL(_ =>
    case CHAR(_,data) =>
    case NCHAR(_,data) =>
    case VARCHAR(_,data) =>
    case NVARCHAR(_,data) =>
    case CLOB(data) =>
    case NCLOB(data) =>
    case BINARY(_,data) =>
    case VARBINARY(_,data) =>
    case BLOB(data) =>
    case BOOLEAN(value) =>
    case BIT(value) =>
    case TINYINT(value) =>
    case SMALLINT(value) =>
    case INTEGER(value) =>
    case BIGINT(value) =>
    case REAL(value) =>
    case DOUBLE(value) =>
    case NUMERIC(value,_,_) =>
    case DECIMAL(value,_,_) =>
    case DATE(date) =>
    case TIME(time) =>
    case TIMESTAMP(timestamp) =>
  */
}
