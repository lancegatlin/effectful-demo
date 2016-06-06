package effectful.examples.pure.dao.sql.impl

import java.time.format.DateTimeFormatter
import javax.xml.bind.DatatypeConverter

import effectful.examples.effects.sql.SqlVal
import effectful.examples.effects.sql.SqlVal._
import effectful.examples.pure.dao.sql.SqlString

object SqlOps {

  def printSql(sqlVal: SqlVal) : SqlString = SqlString {
    def quotes(s: String) = s"'$s'"
    sqlVal match {
      case NULL(_) => "null"
      case CHAR(_,data) => quotes(data.toCharString())
      case VARCHAR(_,data) => quotes(data.toCharString())

      case NCHAR(_,data) => quotes(data.toCharString())
      case NVARCHAR(_,data) => quotes(data.toCharString())

      // todo: prob shouldn't ship these as strings - should be way to attach stream?
      case CLOB(data) => quotes(data.toCharString())
      case NCLOB(data) => quotes(data.toCharString())

      // todo: possible to use base 64 instead of hex for binary? more efficient
      case BINARY(_,data) => DatatypeConverter.printHexBinary(data.toByteArray())
      case VARBINARY(_,data) => DatatypeConverter.printHexBinary(data.toByteArray())
      case BLOB(data) => DatatypeConverter.printHexBinary(data.toByteArray())

      case BOOLEAN(value) => if(value) "true" else "false"
      case BIT(value) => if(value) "1" else "0"
      case TINYINT(value) => value.toString
      case SMALLINT(value) => value.toString
      case INTEGER(value) => value.toString
      case BIGINT(value) => value.toString
      case REAL(value) => value.toString
      case DOUBLE(value) => value.toString
      case NUMERIC(value,_,_) => value.toString
      case DECIMAL(value,_,_) => value.toString
      case DATE(date) => quotes(DateTimeFormatter.ISO_DATE.format(date))
      case TIME(time) => quotes(DateTimeFormatter.ISO_TIME.format(time))
      case TIMESTAMP(timestamp) => quotes(DateTimeFormatter.ISO_INSTANT.format(timestamp))
    }
  }

}
