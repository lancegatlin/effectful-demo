package effectful.examples.adapter.jdbc

import effectful.examples.adapter.jdbc.JdbcSqlDriverOps._
import effectful.examples.effects.sql.SqlDriver._

case class JdbcResultSetCursor(
  id: Symbol,
  resultSet: java.sql.ResultSet,
  onClose: () => Unit
) {

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

  def getCursorMetadata = CursorMetadata(
    // todo: does this work?
    schemaName = metadata.getSchemaName(1),
    // todo: does this work?
    tableName = metadata.getSchemaName(1),
    columns = (1 to columnCount).map { i =>
      import metadata._
      ColumnMetadata(
        name = getColumnName(i),
        label = getColumnLabel(i),
        sqlType = columnTypes(i-1),
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

  def initialCursor : InitialCursor = {
    if(resultSet.isBeforeFirst) {
      InitialCursor.NonEmpty(id)
    } else {
      InitialCursor.Empty
    }
  }

  def next() : Cursor = {
    if(resultSet.next()) {
      Cursor.Row(
        id = id,
        rowNum = resultSet.getRow,
        row =
          (1 to columnCount).map { col =>
            parseResultSetColumn(resultSet,col,columnTypes(col-1))
          }
      )
    } else {
      resultSet.close()
      onClose()
      Cursor.Empty
    }
  }

  def close() : Unit =
    resultSet.close()
}

