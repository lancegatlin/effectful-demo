package effectful.examples.effects.sql

sealed trait SqlType
object SqlType {
//  case object NULL extends SqlType

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
  case object BLOB extends SqlType

  case object BIT extends SqlType
  case object BOOLEAN extends SqlType

//  case class INTEGERP(precision: Int) extends SqlType
  case object TINYINT extends SqlType
  case object SMALLINT extends SqlType // precision = 5
  case object INTEGER extends SqlType // precision = 10
  case object BIGINT extends SqlType // precision = 19

  case object REAL extends SqlType
  case object DOUBLE extends SqlType
  case class NUMERIC(precision: Int, scale: Int) extends SqlType // exact precision
  case class DECIMAL(precision: Int, scale: Int) extends SqlType // db may add more precision

  case object DATE extends SqlType
  case object TIME extends SqlType
  case object TIMESTAMP extends SqlType

  /*
  case CHAR(fixedSize) =>
  case NCHAR(fixedSize) =>
  case VARCHAR(maxSize) =>
  case NVARCHAR(maxSize) =>
  case CLOB =>
  case NCLOB =>
  case BINARY(fixedSize =>
  case VARBINARY(maxSize) =>
  case BLOB =>
  case BIT =>
  case BOOLEAN =>
  case TINYINT =>
  case SMALLINT =>
  case INTEGER =>
  case BIGINT =>
  case REAL =>
  case DOUBLE =>
  case NUMERIC(precision,scale) =>
  case DECIMAL(precision,scale) =>
  case DATE =>
  case TIME =>
  case TIMESTAMP =>
  
   */
}
