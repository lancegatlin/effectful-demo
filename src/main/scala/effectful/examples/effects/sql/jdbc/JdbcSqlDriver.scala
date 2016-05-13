package effectful.examples.effects.sql.jdbc

import java.sql.{Connection => _, PreparedStatement => _, _}
import java.time.{LocalDate, ZoneId, ZoneOffset}

import effectful._
import effectful.examples.effects.sql._
import org.apache.commons.io.IOUtils
import SqlDriver._

object JdbcSqlDriver {
  case class ConnectionInfo(
    url: String,
    username: String,
    password: String
  )
}

class JdbcSqlDriver(
  connectionPool: JdbcSqlDriver.ConnectionInfo => java.sql.Connection
) extends SqlDriver[Id] {
  import JdbcSqlDriver._

  case class InternalConnection(
    connectionInfo: ConnectionInfo,
    jdbcConnection: java.sql.Connection
  ) extends Connection {
    override def url = connectionInfo.url
    override def isClosed =
      jdbcConnection.isClosed
  }

  // Connection

  override def getConnection(
    url: String,
    username: String,
    password: String
  ): Id[Connection] = {
    val connectionInfo = ConnectionInfo(url,username,password)
    InternalConnection(
      connectionInfo,
      connectionPool(connectionInfo)
    )
  }

  override def closeConnection(connection: Connection): Id[Unit] =
    connection.asInstanceOf[InternalConnection].jdbcConnection.close()

  // Transaction

  case class InternalTransaction(
    transactionConnection: InternalConnection,
    mainConnection: InternalConnection
  ) extends Transaction {
    override def isUncommitted: Boolean =
      transactionConnection.isClosed == false
  }

  override def beginTransaction()(implicit context: Context.AutoCommit): Id[Context.InTransaction] = {
    val mainConnection = context.connection.asInstanceOf[InternalConnection]
    // Note: getting another connection from pool for transaction to avoid dealing with "in transaction"
    // state of main auto commit connection
    val transactionConnection = InternalConnection(
      mainConnection.connectionInfo,
      connectionPool(mainConnection.connectionInfo)
    )
    transactionConnection.jdbcConnection.setAutoCommit(false)
    Context.InTransaction(
      transaction = InternalTransaction(
        transactionConnection = transactionConnection,
        mainConnection = mainConnection
      ),
      connection = transactionConnection
    )
  }

  override def rollback()(implicit context: Context.InTransaction): Id[Unit] = {
    context.connection.asInstanceOf[InternalConnection].jdbcConnection.rollback()
  }

  override def commit()(implicit context: Context.InTransaction): Id[Unit] = {
    val c = context.connection.asInstanceOf[InternalConnection].jdbcConnection
    c.commit()
    // Note: releases connection back to connection pool
    c.close()
  }

  // Prepared statement

  case class InternalPreparedStatement(
    statement: String,
    jdbcPreparedStatement: java.sql.PreparedStatement
  ) extends PreparedStatement

  override def prepare(
    statement: String
  )(implicit
    context: Context
  ): Id[PreparedStatement] =
    InternalPreparedStatement(
      statement,
      context.connection.asInstanceOf[InternalConnection].jdbcConnection.prepareStatement(statement)
    )

  override def executePreparedUpdate(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ): Id[Int] = {
    val ps = preparedStatement.asInstanceOf[InternalPreparedStatement].jdbcPreparedStatement
    prepareBatches(ps,rows)
    ps.executeUpdate()
  }

  override def executePreparedQuery(
    preparedStatement: PreparedStatement
  )(
    rows: SqlRow*
  )(implicit
    context: Context
  ): Id[Cursor] = {
    val ps = preparedStatement.asInstanceOf[InternalPreparedStatement].jdbcPreparedStatement
    prepareBatches(ps,rows)
    InternalCursor(
      onNextSeekForward = true,
      resultSet = ps.executeQuery(),
      context = context
    )
  }

  // Immediate execution

  override def executeQuery(
    statement: String
  )(implicit
    context: Context
  ): Id[Cursor] = {
    val s = context.connection.asInstanceOf[InternalConnection].jdbcConnection.createStatement()
    InternalCursor(
      onNextSeekForward = true,
      resultSet = s.executeQuery(statement),
      context = context
    )
  }

  override def executeUpdate(
    statement: String
  )(implicit
    context: Context
  ): Id[Int] = {
    val s = context.connection.asInstanceOf[InternalConnection].jdbcConnection.createStatement()
    s.executeUpdate(statement)
  }

  // Cursor

  case class InternalCursor(
    onNextSeekForward: Boolean,
    resultSet: java.sql.ResultSet,
    context: Context
  ) extends Cursor {
    val metadata = resultSet.getMetaData
    def columnCount = metadata.getColumnCount

    val columnTypes = (1 to columnCount).map { i =>
      import metadata._
      jdbcTypeToSqlType(
        jdbcType = getColumnType(i),
        precision = getPrecision(i),
        scale = getScale(i),
        // todo: fix me
        width = 0 //getColumnDisplaySize()
      )
    }

    lazy val cursorMetadata = CursorMetadata(
      schemaName = metadata.getSchemaName(0),
      tableName = metadata.getSchemaName(0),
      columns = (1 to columnCount).map { i =>
        import metadata._
        ColumnMetadata(
          name = getColumnName(i),
          label = getColumnLabel(i),
          sqlType = columnTypes(i),
          autoIncrement = isAutoIncrement(i),
          caseSensitive = isCaseSensitive(i),
          nullable = isNullable(i) match {
            case java.sql.ResultSetMetaData.columnNoNulls => false
            case _ => true
          },
          signed = isSigned(i)
        )
      }
    )

    def isBeforeFirst = resultSet.isBeforeFirst
    def isFirst = resultSet.isFirst
    def isLast = resultSet.isLast

    def currentRowNum = resultSet.getRow
    def current = (1 to columnCount).map { col =>
      parseResultSetColumn(resultSet,col,columnTypes(col))
    }

    def isClosed = resultSet.isClosed
  }

  override def getMetadata(cursor: Cursor): Id[CursorMetadata] =
    cursor.asInstanceOf[InternalCursor].cursorMetadata

  override def seekAbsolute(cursor: Cursor, rowNum: Int): Id[Cursor] = {
    cursor.asInstanceOf[InternalCursor].resultSet.absolute(rowNum)
    cursor
  }

  override def seekRelative(cursor: Cursor, rowOffset: Int): Id[Cursor] = {
    cursor.asInstanceOf[InternalCursor].resultSet.relative(rowOffset)
    cursor
  }

  override def seekLast(cursor: Cursor): Id[Cursor] = {
    cursor.asInstanceOf[InternalCursor].resultSet.last()
    cursor
  }

  override def seekFirst(cursor: Cursor): Id[Cursor] = {
    cursor.asInstanceOf[InternalCursor].resultSet.first()
    cursor
  }

  override def setSeekDir(cursor: Cursor, forward: Boolean): Id[Cursor] = {
    val fetchDir = if(forward) {
      ResultSet.FETCH_FORWARD
    } else {
      ResultSet.FETCH_REVERSE
    }
    val internalCursor = cursor.asInstanceOf[InternalCursor]
    internalCursor.resultSet.setFetchDirection(fetchDir)
    internalCursor.copy(
      onNextSeekForward = forward
    )
  }

  override def closeCursor(cursor: Cursor): Id[Unit] =
    cursor.asInstanceOf[InternalCursor].resultSet.close()


  override def nextRow(cursor: Cursor): Id[Option[Cursor]] = {
    val internalCursor = cursor.asInstanceOf[InternalCursor]
    if(internalCursor.onNextSeekForward) {
      internalCursor.resultSet.next()
      if(internalCursor.resultSet.isAfterLast() == false) {
        Some(internalCursor)
      } else {
        None
      }
    } else {
      internalCursor.resultSet.previous()
      if(internalCursor.resultSet.isBeforeFirst() == false) {
        Some(internalCursor)
      } else {
        None
      }
    }
  }

  // Utility methods

  def parseResultSetColumn(resultSet: ResultSet, col: Int, sqlType:SqlType) : SqlVal = {
    import SqlType._
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
