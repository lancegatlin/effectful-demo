package effectful.examples.effects.sql.jdbc

import java.sql.{JDBCType, ResultSet, Types}
import java.time.{LocalDate, ZoneId, ZoneOffset}
import effectful.examples.effects.sql._
import effectful.examples.effects.sql.SqlDriver._

object JdbcSqlDriverOps {
  val shouldStreamThreshold = 4096l

  def parseResultSetColumn(resultSet: ResultSet, col: Int, sqlType:SqlType) : SqlVal = {
    def getCharData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        CharData.IsReader(resultSet.getCharacterStream(col))
      } else {
        CharData.IsString(resultSet.getString(col))
      }
    def getNCharData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        CharData.IsReader(resultSet.getNCharacterStream(col))
      } else {
        CharData.IsString(resultSet.getNString(col))
      }
    def getBinData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        BinData.IsBinStream(resultSet.getBinaryStream(col))
      } else {
        BinData.IsByteArray(resultSet.getBytes(col))
      }

    import SqlType._
    val sqlVal =
      sqlType match {
        case CHAR(fixedSize) =>
          SqlVal.CHAR(fixedSize, getCharData(fixedSize, col))
        case NCHAR(fixedSize) =>
          SqlVal.NCHAR(fixedSize, getNCharData(fixedSize, col))
        case VARCHAR(maxSize) =>
          SqlVal.VARCHAR(maxSize, getCharData(maxSize, col))
        case NVARCHAR(maxSize) =>
          SqlVal.NVARCHAR(maxSize, getNCharData(maxSize,col))
        case CLOB =>
          SqlVal.CLOB(getCharData(Long.MaxValue, col))
        case NCLOB =>
          SqlVal.NCLOB(getNCharData(Long.MaxValue, col))
        case BINARY(fixedSize) =>
          SqlVal.BINARY(fixedSize, getBinData(fixedSize, col))
        case VARBINARY(maxSize) =>
          SqlVal.VARBINARY(maxSize, getBinData(maxSize, col))
        case BLOB =>
          SqlVal.BLOB(getBinData(Long.MaxValue, col))
        case BIT =>
          SqlVal.BIT(resultSet.getBoolean(col))
        case BOOLEAN =>
          SqlVal.BOOLEAN(resultSet.getBoolean(col))
        case TINYINT =>
          SqlVal.TINYINT(resultSet.getShort(col))
        case SMALLINT =>
          SqlVal.SMALLINT(resultSet.getShort(col))
        case INTEGER =>
          SqlVal.INTEGER(resultSet.getInt(col))
        case BIGINT =>
          SqlVal.BIGINT(resultSet.getLong(col))
        case REAL =>
          SqlVal.REAL(resultSet.getFloat(col))
        case DOUBLE =>
          SqlVal.DOUBLE(resultSet.getDouble(col))
        case NUMERIC(precision,scale) =>
          SqlVal.NUMERIC(BigDecimal(resultSet.getBigDecimal(col)),precision,scale)
        case DECIMAL(precision,scale) =>
          SqlVal.DECIMAL(BigDecimal(resultSet.getBigDecimal(col)),precision,scale)
        case DATE =>
          val oldDate = resultSet.getDate(col)
          val newDate = oldDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
          SqlVal.DATE(newDate)
        case TIME =>
          val oldTime = resultSet.getTime(col)
          val newTime = oldTime.toInstant.atZone(ZoneId.systemDefault()).toLocalTime
          SqlVal.TIME(newTime)
        case TIMESTAMP =>
          SqlVal.TIMESTAMP(java.time.Instant.ofEpochMilli(resultSet.getTimestamp(col).getTime))
      }
    if(resultSet.wasNull) {
      SqlVal.NULL(sqlType)
    } else {
      sqlVal
    }
  }

  def prepareBatches(ps: java.sql.PreparedStatement, rows: Seq[SqlRow]) : Unit = {
    def setCharData(i: Int, data: CharData) : Unit = {
      data match {
        case CharData.IsReader(reader) =>
          ps.setCharacterStream(i,reader)
        case CharData.IsString(s) =>
          ps.setString(i,s)
      }
    }
    def setBinData(i: Int, data: BinData) : Unit = {
      data match {
        case BinData.IsBinStream(bin) =>
          ps.setBinaryStream(i,bin)
        case BinData.IsByteArray(bytes) =>
          ps.setBytes(i,bytes)
      }
    }

    rows.foreach { row =>
      import SqlVal._
      row.iterator.zipWithIndex.foreach { case (sqlVal,i) =>
        sqlVal match {
          case NULL(sqlType) =>
            ps.setNull(i,sqlTypeToJdbcType(sqlType).ordinal)
          case CHAR(_,data) =>
            setCharData(i,data)
          case NCHAR(_,data) =>
            setCharData(i,data)
          case VARCHAR(_,data) =>
            setCharData(i,data)
          case NVARCHAR(_,data) =>
            setCharData(i,data)
          case CLOB(data) =>
            ps.setClob(i,data.toCharStream())
          case NCLOB(data) =>
            ps.setNClob(i,data.toCharStream())
          case BINARY(_,data) =>
            setBinData(i,data)
          case VARBINARY(_,data) =>
            setBinData(i,data)
          case BLOB(data) =>
            ps.setBlob(i,data.toBinStream())
          case BOOLEAN(value) =>
            ps.setBoolean(i,value)
          case BIT(value) =>
            ps.setBoolean(i,value)
          case TINYINT(value) =>
            ps.setShort(i,value)
          case SMALLINT(value) =>
            ps.setShort(i,value)
          case INTEGER(value) =>
            ps.setInt(i,value)
          case BIGINT(value) =>
            ps.setLong(i,value)
          case REAL(value) =>
            ps.setFloat(i,value)
          case DOUBLE(value) =>
            ps.setDouble(i,value)
          case NUMERIC(value,_,_) =>
            ps.setBigDecimal(i,value.underlying())
          case DECIMAL(value,_,_) =>
            ps.setBigDecimal(i,value.underlying())
          case DATE(date) =>
            ps.setDate(i, new java.sql.Date(
              // todo: does this work??
              date.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli
            ))
          case TIME(time) =>
            ps.setTime(i, new java.sql.Time(
              // todo: does this work??
              time.atDate(LocalDate.ofEpochDay(0)).toInstant(ZoneOffset.UTC).toEpochMilli
            ))
          case TIMESTAMP(timestamp) =>
        }
      }
      ps.addBatch()
    }
  }

  def sqlTypeToJdbcType : SqlType => JDBCType = {
    import SqlType._
    import java.sql.JDBCType

    {
      case CHAR(fixedSize) => JDBCType.CHAR
      case NCHAR(fixedSize) => JDBCType.NCHAR
      case VARCHAR(maxSize) => JDBCType.VARCHAR
      case NVARCHAR(maxSize) => JDBCType.NVARCHAR
      case CLOB => JDBCType.CLOB
      case NCLOB => JDBCType.NCLOB
      case BINARY(fixedSize) => JDBCType.BINARY
      case VARBINARY(maxSize) => JDBCType.VARBINARY
      case BLOB => JDBCType.BLOB
      case BIT => JDBCType.BIT
      case BOOLEAN => JDBCType.BOOLEAN
      case TINYINT => JDBCType.TINYINT
      case SMALLINT => JDBCType.SMALLINT
      case INTEGER => JDBCType.INTEGER
      case BIGINT => JDBCType.BIGINT
      case REAL => JDBCType.REAL
      case DOUBLE => JDBCType.DOUBLE
      case NUMERIC(precision,scale) => JDBCType.NUMERIC
      case DECIMAL(precision,scale) => JDBCType.DECIMAL
      case DATE => JDBCType.DATE
      case TIME => JDBCType.TIME
      case TIMESTAMP => JDBCType.TIMESTAMP
    }
  }

  def jdbcTypeToSqlType(
    jdbcType: Int,
    scale: Int,
    precision: Int,
    width: Int
  ) : SqlType = {
    import SqlType._

    jdbcType match {
      case Types.CHAR => CHAR(width)
      case Types.NCHAR => NCHAR(width)
      case Types.VARCHAR => VARCHAR(width)
      case Types.NVARCHAR => NVARCHAR(width)
      case Types.CLOB => CLOB
      case Types.NCLOB => NCLOB
      case Types.BINARY => BINARY(width)
      case Types.VARBINARY => VARBINARY(width)
      case Types.BLOB => BLOB
      case Types.BIT => BIT
      case Types.BOOLEAN => BOOLEAN
      case Types.TINYINT => TINYINT
      case Types.SMALLINT => SMALLINT
      case Types.INTEGER => INTEGER
      case Types.BIGINT => BIGINT
      case Types.REAL => REAL
      case Types.DOUBLE => DOUBLE
      case Types.NUMERIC => NUMERIC(precision,scale)
      case Types.DECIMAL => DECIMAL(precision,scale)
      case Types.DATE => DATE
      case Types.TIME => TIME
      case Types.TIMESTAMP => TIMESTAMP
    }
  }
}
