package effectful.examples.effects.sql.jdbc

import java.sql.{JDBCType, ResultSet, Types}
import java.time.{LocalDate, ZoneId, ZoneOffset}

import org.apache.commons.io.IOUtils
import effectful.examples.effects.sql._
import effectful.examples.effects.sql.SqlDriver._

object JdbcSqlDriverOps {
  def parseResultSetColumn(resultSet: ResultSet, col: Int, sqlType:SqlType) : SqlVal = {
    import SqlType._
    val sqlVal =
      sqlType match {
        case CHAR(fixedSize) =>
          SqlVal.CHAR(fixedSize, resultSet.getString(col))
          // todo: stream "big" strings
        case NCHAR(fixedSize) =>
          SqlVal.NCHAR(fixedSize, resultSet.getString(col))
        case VARCHAR(maxSize) =>
          SqlVal.NCHAR(maxSize, resultSet.getString(col))
        case NVARCHAR(maxSize) =>
          SqlVal.NVARCHAR(maxSize, resultSet.getString(col))
        case CLOB | NCLOB =>
          val toCharStream = () => resultSet.getClob(col).getCharacterStream
          val toCharString = () => IOUtils.toString(toCharStream())
          SqlVal.CLOB()(
            toCharStream = toCharStream,
            toCharString = toCharString
          )
        case BINARY(fixedSize) =>
          val toBinaryStream = () => resultSet.getBinaryStream(col)
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.BINARY(fixedSize)(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
        case VARBINARY(maxSize) =>
          val toBinaryStream = () => resultSet.getBinaryStream(col)
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.VARBINARY(maxSize)(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
        case BLOB =>
          val toBinaryStream = () => resultSet.getBlob(col).getBinaryStream
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.BLOB()(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
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
    rows.foreach { row =>
      import SqlVal._
      row.iterator.zipWithIndex.foreach { case (sqlVal,i) =>
        sqlVal match {
          case NULL(sqlType) =>
            ps.setNull(i,sqlTypeToJdbcType(sqlType).ordinal)
          case sql@CHAR(_) =>
            // todo: use stream for "big" strings
            ps.setString(i,sql.toCharString())
          case sql@NCHAR(_) =>
            ps.setString(i,sql.toCharString())
          case sql@VARCHAR(_) =>
            ps.setString(i,sql.toCharString())
          case sql@NVARCHAR(_) =>
            ps.setString(i,sql.toCharString())
          case sql@CLOB() =>
            ps.setClob(i,sql.toCharStream())
          case sql@NCLOB() =>
            ps.setClob(i,sql.toCharStream())
          case sql@BINARY(_) =>
            ps.setBlob(i,sql.toBinaryStream())
          case sql@VARBINARY(_) =>
            ps.setBlob(i,sql.toBinaryStream())
          case sql@BLOB() =>
            ps.setBlob(i,sql.toBinaryStream())
          case BOOLEAN(value) =>
            ps.setBoolean(i,value)
          case BIT(value: Boolean) =>
            ps.setBoolean(i,value)
          case TINYINT(value: Short) =>
            ps.setShort(i,value)
          case SMALLINT(value: Short) =>
            ps.setShort(i,value)
          case INTEGER(value: Int) =>
            ps.setInt(i,value)
          case BIGINT(value: Long) =>
            ps.setLong(i,value)
          case REAL(value: Float) =>
            ps.setFloat(i,value)
          case DOUBLE(value: Double) =>
            ps.setDouble(i,value)
          case sql@NUMERIC(value,_,_) =>
            ps.setBigDecimal(i,value.underlying())
          case sql@DECIMAL(value,_,_) =>
            ps.setBigDecimal(i,value.underlying())
          case sql@DATE(date) =>
            ps.setDate(i, new java.sql.Date(
              // todo: does this work??
              date.atStartOfDay(ZoneId.systemDefault()).toInstant.toEpochMilli
            ))
          case sql@TIME(time) =>
            ps.setTime(i, new java.sql.Time(
              // todo: does this work??
              time.atDate(LocalDate.ofEpochDay(0)).toInstant(ZoneOffset.UTC).toEpochMilli
            ))
          case sql@TIMESTAMP(timestamp) =>
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
