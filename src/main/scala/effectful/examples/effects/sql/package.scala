package effectful.examples.effects

import scala.language.higherKinds
import scala.language.implicitConversions
import java.time.format.DateTimeFormatter
import javax.xml.bind.DatatypeConverter
import effectful._

package object sql {
  implicit def connectionToContextAutoCommit(implicit
    connection: SqlDriver.ConnectionPool
  ) : SqlDriver.Context.AutoCommit =
    SqlDriver.Context.AutoCommit(connection)

  implicit class SqlCursorPML[E[_]](val self: SqlDriver[E]) extends AnyVal {
    def iterator(cursor: SqlDriver.Cursor)(implicit E:EffectSystem[E]) : EffectInputStream[E,SqlDriver.SqlRow]= {
      val _E = E
      new EffectInputStream[E,SqlDriver.SqlRow] {
        implicit val E = _E
        override def next() = self.nextCursor(cursor).map {
          case SqlDriver.Cursor.Row(_,_,row) => Some(row)
          case SqlDriver.Cursor.Empty(_) => None
        }
      }
    }
  }

  implicit class SqlValPML(val self: SqlVal) extends AnyVal {
    def printSQL : String = {
      def quotes(s: String) = s"'$s'"

      import SqlVal._
      self match {
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
}
