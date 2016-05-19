package effectful.examples.effects

import scala.language.higherKinds
import scala.language.implicitConversions
import java.time.format.DateTimeFormatter
import javax.xml.bind.DatatypeConverter
import effectful._
import effectful.examples.effects.sql.SqlDriver._

package object sql {
//  implicit def connectionToContextAutoCommit(implicit
//    connection: SqlDriver.ConnectionPool
//  ) : SqlDriver.Context.AutoCommit =
//    SqlDriver.Context.AutoCommit(connection)

  implicit class SqlCursorPML[E[_]](val self: SqlDriver[E]) extends AnyVal {
    def iteratePreparedQuery(
      preparedStatementId: PreparedStatementId
    )(
      rows: SqlRow*
    )(implicit
      E:EffectSystem[E]
    ) : EffectIterator[E,SqlRow] = {
      iterator({ () => self.executePreparedQuery(preparedStatementId)(rows:_*) })
    }

    def iterateQuery(
      statement: String
    )(implicit
      context: Context,
      E:EffectSystem[E]
    ) : EffectIterator[E,SqlRow] = {
      iterator({ () => self.executeQuery(statement) })
    }

    def iterator(fetchCursor: () => E[SqlDriver.Cursor])(implicit E:EffectSystem[E]) : EffectIterator[E,SqlRow]= {
      EffectIterator.flatten {
        for {
          cursor <- fetchCursor()
        } yield EffectIterator[E,SqlRow] { () =>
          self.nextCursor(cursor.id).map {
            case Cursor.Row(_,_,row) => Some(row)
            case Cursor.Empty(_) => None
          }
        }
      }
    }

    def autoCommit[A](
      f: Context.AutoCommit.type => E[A]
    )(implicit
      e:EffectSystem[E]
    ) : E[A] = f(Context.AutoCommit)

    def inTransaction[A](
      f: Context.InTransaction => E[A]
    )(implicit
      E:EffectSystem[E]
    ) : E[A] =
      for {
        transaction <- self.beginTransaction()
        result <- {
          E.Try {
            for {
              result <- f(transaction)
              _ <- self.commit()(transaction)
            } yield result
          }{
            case t : Throwable =>
              for {
                _ <- self.rollback()(transaction)
              } yield throw t
          }
        }
      } yield result
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
