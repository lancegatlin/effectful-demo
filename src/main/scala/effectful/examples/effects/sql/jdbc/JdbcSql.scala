//package effectful.examples.effects.sql.jdbc
//
//import java.sql.DriverManager
//
//import effectful._
//import effectful.examples.effects.sql.Sql
//
//class JdbcSql extends Sql[Id] {
//  type Connection = java.sql.Connection
//  type PreparedStatement = java.sql.PreparedStatement
//
//
//  class Row(underlying: java.sql.ResultSet) extends super.Row {
//    override def apply(i: Int): SqlVal =
//
//  }
//  class ResultSet(underlying: java.sql.ResultSet) extends super.ResultSet {
//    override def columnCount: Int =
//      underlying.getMetaData.getColumnCount
//    override def columnLabel(i: Int): String =
//      underlying.getMetaData.getColumnLabel(i+1)
//    override def columnName(i: Int): String =
//      underlying.getMetaData.getColumnName(i+1)
//
//    override def apply(i: Int): Row =
//      underlying.absolute()
//
//    override def iterator: Iterator[Row] = ???
//  }
//  override def getConnection(url: String, username: String, password: String): Id[Connection] =
//    DriverManager.getConnection(url,username,password)
//
//  override def execute(preparedStatement: PreparedStatement, args: SqlVal*): Id[ResultSet] = {
//    preparedStatement.executeQuery()
//  }
//
//
//  override def prepare(connection: Connection, statement: String): Id[PreparedStatement] =
//    connection.prepareStatement(statement)
//}
