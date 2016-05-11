package effectful.examples.effects.sql.jdbc

import java.sql.{Connection => _, PreparedStatement => _, _}
import java.time.{LocalDate, ZoneId, ZoneOffset}

import effectful._
import effectful.examples.effects.sql._
import org.apache.commons.io.IOUtils

class JdbcSql extends Sql[Id] {
  case class JdbcConnection(
    url: String,
    jdbcConnection: java.sql.Connection
  ) extends Connection {
    override def isClosed =
      jdbcConnection.isClosed
    override def close() =
      jdbcConnection.close()
    override def commit(): Unit =
      jdbcConnection.commit()
  }

  case class JdbcPreparedStatement(
    statement: String,
    preparedStatement: java.sql.PreparedStatement
  ) extends PreparedStatement

  override def getConnection(
    url: String,
    username: String,
    password: String
  ): Id[Connection] =
    JdbcConnection(
      url,
      DriverManager.getConnection(url,username,password)
    )

  override def prepare(
    connection: Connection,
    statement: String
  ): Id[PreparedStatement] =
    JdbcPreparedStatement(
      statement,
      connection.asInstanceOf[JdbcConnection].jdbcConnection.prepareStatement(statement)
    )

  override def executePreparedUpdate(
    preparedStatement: PreparedStatement,
    batches: Seq[SqlVal]*
  ): Id[Int] = {
    val ps = preparedStatement.asInstanceOf[JdbcPreparedStatement].preparedStatement
    prepareBatches(ps,batches)
    ps.executeUpdate()
  }


  override def executePreparedQuery(
    preparedStatement: PreparedStatement,
    batches: Seq[SqlVal]*
  ): Id[Cursor] = {
    val ps = preparedStatement.asInstanceOf[JdbcPreparedStatement].preparedStatement
    prepareBatches(ps,batches)
    JdbcCursor(ps.executeQuery())
  }

  override def executeQuery(
    connection: Connection,
    statement: String
  ): Id[Cursor] = {
    val s = connection.asInstanceOf[JdbcConnection].jdbcConnection.createStatement()
    JdbcCursor(s.executeQuery(statement))
  }

  override def executeUpdate(
    connection: Connection,
    statement: String
  ): Id[Int] = {
    val s = connection.asInstanceOf[JdbcConnection].jdbcConnection.createStatement()
    s.executeUpdate(statement)
  }

  case class JdbcRow(
    index: Int,
    cursor: JdbcCursor
  ) extends Row {
    def apply(col: Int) : SqlVal = {
      import SqlType._
      cursor.columnMetadata(col).sqlType match {
        case CHAR(fixedSize) =>
          SqlVal.CHAR(fixedSize, cursor.resultSet.getString(col))
          // todo: stream "big" strings
        case NCHAR(fixedSize) =>
          SqlVal.NCHAR(fixedSize, cursor.resultSet.getString(col))
        case VARCHAR(maxSize) =>
          SqlVal.NCHAR(maxSize, cursor.resultSet.getString(col))
        case NVARCHAR(maxSize) =>
          SqlVal.NVARCHAR(maxSize, cursor.resultSet.getString(col))
        case CLOB | NCLOB =>
          val toCharStream = () => cursor.resultSet.getClob(col).getCharacterStream
          val toCharString = () => IOUtils.toString(toCharStream())
          SqlVal.CLOB()(
            toCharStream = toCharStream,
            toCharString = toCharString
          )
        case BINARY(fixedSize) =>
          val toBinaryStream = () => cursor.resultSet.getBinaryStream(col)
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.BINARY(fixedSize)(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
        case VARBINARY(maxSize) =>
          val toBinaryStream = () => cursor.resultSet.getBinaryStream(col)
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.VARBINARY(maxSize)(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
        case BLOB =>
          val toBinaryStream = () => cursor.resultSet.getBlob(col).getBinaryStream
          val toByteArray = () => IOUtils.toByteArray(toBinaryStream())
          SqlVal.BLOB()(
            toBinaryStream = toBinaryStream,
            toByteArray = toByteArray
          )
        case BIT =>
          SqlVal.BIT(cursor.resultSet.getBoolean(col))
        case BOOLEAN =>
          SqlVal.BOOLEAN(cursor.resultSet.getBoolean(col))
        case TINYINT =>
          SqlVal.TINYINT(cursor.resultSet.getShort(col))
        case SMALLINT =>
          SqlVal.SMALLINT(cursor.resultSet.getShort(col))
        case INTEGER =>
          SqlVal.INTEGER(cursor.resultSet.getInt(col))
        case BIGINT =>
          SqlVal.BIGINT(cursor.resultSet.getLong(col))
        case REAL =>
          SqlVal.REAL(cursor.resultSet.getFloat(col))
        case DOUBLE =>
          SqlVal.DOUBLE(cursor.resultSet.getDouble(col))
        case NUMERIC(precision,scale) =>
          SqlVal.NUMERIC(BigDecimal(cursor.resultSet.getBigDecimal(col)),precision,scale)
        case DECIMAL(precision,scale) =>
          SqlVal.DECIMAL(BigDecimal(cursor.resultSet.getBigDecimal(col)),precision,scale)
        case DATE =>
          val oldDate = cursor.resultSet.getDate(col)
          val newDate = oldDate.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
          SqlVal.DATE(newDate)
        case TIME =>
          val oldTime = cursor.resultSet.getTime(col)
          val newTime = oldTime.toInstant.atZone(ZoneId.systemDefault()).toLocalTime
          SqlVal.TIME(newTime)
        case TIMESTAMP =>
          SqlVal.TIMESTAMP(java.time.Instant.ofEpochMilli(cursor.resultSet.getTimestamp(col).getTime))
      }
    }
  }
  case class JdbcCursor(
    resultSet: java.sql.ResultSet
  ) extends Cursor {
    val metadata = resultSet.getMetaData
    // todo: does 0 work here? why is column index even an input?
    def schemaName = metadata.getSchemaName(0)
    def tableName = metadata.getTableName(0)

    def columnCount = metadata.getColumnCount

    lazy val columnMetadata = (0 until columnCount).map { i =>
      import metadata._
      ColumnMetadata(
        name = getColumnName(i),
        label = getColumnLabel(i),
        sqlType = jdbcTypeToSqlType(
          jdbcType = getColumnType(i),
          precision = getPrecision(i),
          scale = getScale(i),
          // todo: fix me
          width = 0 //getColumnDisplaySize()
        ),
        autoIncrement = isAutoIncrement(i),
        caseSensitive = isCaseSensitive(i),
        nullable = isNullable(i) match {
          case java.sql.ResultSetMetaData.columnNoNulls => false
          case _ => true
        },
        signed = isSigned(i)
      )
    }
    def columns(i: Int) = columnMetadata(i)

    def first() = resultSet.first()
    def last() = resultSet.last()
    def seekAbsolute(rowNum: Int) = resultSet.absolute(rowNum)
    def seekRelative(rowOffset: Int) = resultSet.relative(rowOffset)

    def isBeforeFirst = resultSet.isBeforeFirst
    def isFirst = resultSet.isFirst
    def isLast = resultSet.isLast

    def currentRowNum = resultSet.getRow
    def current = JdbcRow(currentRowNum,this)

    def reverse() : Unit = ???

    def isClosed = resultSet.isClosed
    def close() = resultSet.close()

    def hasNext = isLast
    def next() = {
      resultSet.next()
      current
    }
  }

  def prepareBatches(ps: java.sql.PreparedStatement, batches: Seq[Seq[SqlVal]]) : Unit = {
    batches.foreach { row =>
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
