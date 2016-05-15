package effectful.examples.effects

import scala.language.higherKinds
import scala.language.implicitConversions
import java.time.format.DateTimeFormatter
import javax.xml.bind.DatatypeConverter
import effectful._

package object sql {
  implicit def connectionToContextAutoCommit(implicit
    connection: SqlDriver.Connection
  ) : SqlDriver.Context.AutoCommit =
    SqlDriver.Context.AutoCommit(connection)

  implicit class SqlCursorPML[E[_]](val self: SqlDriver[E]) extends AnyVal {
    def iterator(cursor: SqlDriver.Cursor)(implicit E:EffectSystem[E]) : EffectInputStream[E,SqlDriver.SqlRow]= {
      val _E = E
      new EffectInputStream[E,SqlDriver.SqlRow] {
        implicit val E = _E
        override def next() = self.nextRow(cursor).map {
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
        case sql@CHAR(_) => quotes(sql.toCharString())
        case sql@VARCHAR(_) => quotes(sql.toCharString())

        case sql@NCHAR(_) => quotes(sql.toCharString())
        case sql@NVARCHAR(_) => quotes(sql.toCharString())

        // todo: prob shouldn't ship these as strings - should be way to attach stream?
        case sql@CLOB() => quotes(sql.toCharString())
        case sql@NCLOB() => quotes(sql.toCharString())

        // todo: possible to use base 64 instead of hex for binary? more efficient
        case sql@BINARY(_) => DatatypeConverter.printHexBinary(sql.toByteArray())
        case sql@VARBINARY(_) => DatatypeConverter.printHexBinary(sql.toByteArray())
        case sql@BLOB() => DatatypeConverter.printHexBinary(sql.toByteArray())

        case BOOLEAN(value) => if(value) "true" else "false"
        case BIT(value: Boolean) => if(value) "1" else "0"
        case TINYINT(value: Short) => value.toString
        case SMALLINT(value: Short) => value.toString
        case INTEGER(value: Int) => value.toString
        case BIGINT(value: Long) => value.toString
        case REAL(value: Float) => value.toString
        case DOUBLE(value: Double) => value.toString
        case sql@NUMERIC(value,_,_) => value.toString
        case sql@DECIMAL(value,_,_) => value.toString
        case sql@DATE(date) => quotes(DateTimeFormatter.ISO_DATE.format(date))
        case sql@TIME(time) => quotes(DateTimeFormatter.ISO_TIME.format(time))
        case sql@TIMESTAMP(timestamp) => quotes(DateTimeFormatter.ISO_INSTANT.format(timestamp))
      }
    }
  }
}
