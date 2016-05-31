package effectful.examples.adapter.jdbc

import java.sql.{JDBCType, ResultSet, Types}
import java.time.{LocalDate, ZoneId, ZoneOffset}

import effectful.examples.effects.sql.SqlDriver._
import effectful.examples.effects.sql._

object JdbcSqlDriverOps {
  val shouldStreamThreshold = 4096l

  def parseResultSetColumn(resultSet: ResultSet, col: Int, sqlType:SqlType) : SqlVal = {
    def getCharData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        Option(resultSet.getCharacterStream(col)).map(CharData.IsReader)
      } else {
        Option(resultSet.getString(col)).map(CharData.IsString)
      }
    def getNCharData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        Option(resultSet.getNCharacterStream(col)).map(CharData.IsReader)
      } else {
        Option(resultSet.getNString(col)).map(CharData.IsString)
      }
    def getBinData(size: Long, col: Int) =
      if(size > shouldStreamThreshold) {
        Option(resultSet.getBinaryStream(col)).map(BinData.IsBinStream)
      } else {
        Option(resultSet.getBytes(col)).map(BinData.IsByteArray)
      }

    import SqlType._
    val optSqlVal =
      sqlType match {
        case CHAR(fixedSize) =>
          getCharData(fixedSize, col).map(SqlVal.CHAR(fixedSize, _))
        case NCHAR(fixedSize) =>
          getNCharData(fixedSize, col).map(SqlVal.NCHAR(fixedSize, _))
        case VARCHAR(maxSize) =>
          getCharData(maxSize, col).map(SqlVal.VARCHAR(maxSize, _))
        case NVARCHAR(maxSize) =>
          getNCharData(maxSize,col).map(SqlVal.NVARCHAR(maxSize, _))
        case CLOB =>
          getCharData(Long.MaxValue, col).map(SqlVal.CLOB)
        case NCLOB =>
          getNCharData(Long.MaxValue, col).map(SqlVal.NCLOB)
        case BINARY(fixedSize) =>
          getBinData(fixedSize, col).map(SqlVal.BINARY(fixedSize, _))
        case VARBINARY(maxSize) =>
          getBinData(maxSize, col).map(SqlVal.VARBINARY(maxSize, _))
        case BLOB =>
          getBinData(Long.MaxValue, col).map(SqlVal.BLOB)
        case BIT =>
          Some(SqlVal.BIT(resultSet.getBoolean(col)))
        case BOOLEAN =>
          Some(SqlVal.BOOLEAN(resultSet.getBoolean(col)))
        case TINYINT =>
          Some(SqlVal.TINYINT(resultSet.getShort(col)))
        case SMALLINT =>
          Some(SqlVal.SMALLINT(resultSet.getShort(col)))
        case INTEGER =>
          Some(SqlVal.INTEGER(resultSet.getInt(col)))
        case BIGINT =>
          Some(SqlVal.BIGINT(resultSet.getLong(col)))
        case REAL =>
          Some(SqlVal.REAL(resultSet.getFloat(col)))
        case DOUBLE =>
          Some(SqlVal.DOUBLE(resultSet.getDouble(col)))
        case NUMERIC(precision,scale) =>
          Option(resultSet.getBigDecimal(col)).map(n => SqlVal.NUMERIC(BigDecimal(n),precision,scale))
        case DECIMAL(precision,scale) =>
          Option(resultSet.getBigDecimal(col)).map(n => SqlVal.DECIMAL(BigDecimal(n),precision,scale))
        case DATE =>
          Option(resultSet.getDate(col)).map { oldDate =>
            val newDate = oldDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
            SqlVal.DATE(newDate)
          }
        case TIME =>
          Option(resultSet.getTime(col)).map { oldTime =>
            val newTime = oldTime.toInstant.atZone(ZoneId.systemDefault()).toLocalTime
            SqlVal.TIME(newTime)
          }
        case TIMESTAMP =>
          Option(resultSet.getTimestamp(col)).map { timestamp =>
            SqlVal.TIMESTAMP(java.time.Instant.ofEpochMilli(timestamp.getTime))
          }
      }

    optSqlVal match {
      case Some(sqlVal) =>
        if(resultSet.wasNull) {
          SqlVal.NULL(sqlType)
        } else {
          sqlVal
        }
      case None =>
        SqlVal.NULL(sqlType)
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
      row.iterator.zipWithIndex.foreach { case (sqlVal,_i) =>
        val i = _i + 1
        sqlVal match {
          case NULL(sqlType) =>
            // todo: fixing this up to use metadata to lookup type of column
            // todo: would allow removing SqlVal.sqlType which causes a bunch
            // todo: of awkwardness in other parts of the code
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
            ps.setTimestamp(i, new java.sql.Timestamp(
              timestamp.toEpochMilli
            ))
        }
      }
      ps.addBatch()
    }
  }

  def sqlTypeToJdbcType : SqlType => JDBCType = {
    import java.sql.JDBCType

    import SqlType._

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
    width: Long
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
