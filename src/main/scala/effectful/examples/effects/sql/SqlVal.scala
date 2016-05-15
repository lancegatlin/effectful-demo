package effectful.examples.effects.sql

import org.apache.commons.io.IOUtils

sealed trait SqlVal {
  def sqlType: SqlType
}
object SqlVal {
  // Mappings based on: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
  // todo: support SqlTypes & Vals that are closer to underlying data instead of as interpreted by JDBC

  case class NULL(sqlType: SqlType) extends SqlVal
  case class CHAR(
    fixedLength: Long
  )(
    // todo: use sealed trait union type here of String | Reader that can convert to either
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.CHAR(fixedLength)
  }
  object CHAR {
    def apply(value: String) : CHAR = CHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
    def apply(fixedLength: Long, value: String) : CHAR = CHAR(fixedLength)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class NCHAR(
    fixedLength: Long
  )(
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.NCHAR(fixedLength)
  }
  object NCHAR {
    def apply(value: String) : NCHAR = NCHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
    def apply(fixedLength: Long, value: String) : NCHAR = NCHAR(fixedLength)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class VARCHAR(
    maxLength: Long
  )(
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.VARCHAR(maxLength)
  }
  object VARCHAR {
    def apply(value: String) : VARCHAR = VARCHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
    def apply(maxLength: Long, value: String) : VARCHAR = VARCHAR(maxLength)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class NVARCHAR(
    maxLength: Long
  )(
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.NVARCHAR(maxLength)
  }
  object NVARCHAR {
    def apply(value: String) : NVARCHAR = NVARCHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
    def apply(maxLength: Long, value: String) : NVARCHAR = NVARCHAR(maxLength)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class CLOB()(
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.CLOB
  }
  object CLOB {
    def apply(value: String) : VARCHAR = VARCHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class NCLOB()(
    val toCharStream: () => java.io.Reader,
    val toCharString: () => String
  ) extends SqlVal {
    def sqlType = SqlType.NCLOB
  }
  object NCLOB {
    def apply(value: String) : VARCHAR = VARCHAR(0)(
      toCharStream = () => new java.io.StringReader(value),
      toCharString = () => value
    )
  }
  case class BINARY(
    fixedSize: Long
  )(
    // todo: use sealed trait union type here of Array[Byte] | InputStream that can convert to either
    val toBinaryStream: () => java.io.InputStream,
    val toByteArray: () => Array[Byte]
  ) extends SqlVal {
    def sqlType = SqlType.BINARY(fixedSize)
  }
  object BINARY {
    def apply(bin: java.io.InputStream) : BINARY = BINARY(0)(
      toBinaryStream = () => bin,
      toByteArray = () => IOUtils.toByteArray(bin)
    )
    def apply(bytes: Array[Byte]) : BINARY = BINARY(0)(
      toBinaryStream = () => new java.io.ByteArrayInputStream(bytes),
      toByteArray = () => bytes
    )
  }
  case class VARBINARY(
    maxSize: Long
  )(
    val toBinaryStream: () => java.io.InputStream,
    val toByteArray: () => Array[Byte]
  ) extends SqlVal {
    def sqlType = SqlType.VARBINARY(maxSize)
  }
  object VARBINARY {
    def apply(bin: java.io.InputStream) : BINARY = BINARY(0)(
      toBinaryStream = () => bin,
      toByteArray = () => IOUtils.toByteArray(bin)
    )
    def apply(bytes: Array[Byte]) : BINARY = BINARY(0)(
      toBinaryStream = () => new java.io.ByteArrayInputStream(bytes),
      toByteArray = () => bytes
    )
  }
  case class BLOB()(
    val toBinaryStream: () => java.io.InputStream,
    val toByteArray: () => Array[Byte]
  ) extends SqlVal {
    def sqlType = SqlType.BLOB
  }
  object BLOB {
    def apply(bin: java.io.InputStream) : BINARY = BINARY(0)(
      toBinaryStream = () => bin,
      toByteArray = () => IOUtils.toByteArray(bin)
    )
    def apply(bytes: Array[Byte]) : BINARY = BINARY(0)(
      toBinaryStream = () => new java.io.ByteArrayInputStream(bytes),
      toByteArray = () => bytes
    )
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
    case sql@CHAR(_) =>
    case sql@VARCHAR(_) =>
    case sql@CLOB() =>
    case sql@NCLOB() =>
    case sql@BINARY(_) =>
    case sql@VARBINARY(_) =>
    case sql@BLOB() =>
    case BOOLEAN(value) =>
    case BIT(value: Boolean) =>
    case TINYINT(value: Short) =>
    case SMALLINT(value: Short) =>
    case INTEGER(value: Int) =>
    case BIGINT(value: Long) =>
    case REAL(value: Float) =>
    case DOUBLE(value: Double) =>
    case sql@NUMERIC(value,_,_) =>
    case sql@DECIMAL(value,_,_) =>
    case sql@DATE(date) =>
    case sql@TIME(time) =>
    case sql@TIMESTAMP(timestamp) =>
  */
}
