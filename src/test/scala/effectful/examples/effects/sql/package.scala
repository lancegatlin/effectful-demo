package effectful.examples.effects

import java.time.format.DateTimeFormatter
import javax.xml.bind.DatatypeConverter

import effectful._
import effectful.aspects.Exceptions
import effectful.cats.Monad
import effectful.examples.effects.sql.SqlDriver._

package object sql {
  implicit class SqlCursorPML[E[_]](val self: SqlDriver[E]) extends AnyVal {
    def iteratePreparedQuery(
      preparedStatementId: PreparedStatementId
    )(
      rows: SqlRow*
    )(implicit
      E:Monad[E]
    ) : EffectIterator[E,SqlRow] = {
      iterator({ () => self.executePreparedQuery(preparedStatementId)(rows:_*) })
    }

    def iterateQuery(
      statement: String
    )(implicit
      context: Context,
      E:Monad[E]
    ) : EffectIterator[E,SqlRow] = {
      iterator({ () => self.executeQuery(statement) })
    }

    def iterator(fetchCursor: () => E[SqlDriver.Cursor])(implicit E:Monad[E]) : EffectIterator[E,SqlRow]= {
      import Monad.ops._

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
      E:Monad[E]
    ) : E[A] = f(Context.AutoCommit)

    def inTransaction[A](
      f: Context.InTransaction => E[A]
    )(implicit
      E:Monad[E],
      X:Exceptions[E]
    ) : E[A] = {
      import Monad.ops._
      import X._

      for {
        transaction <- self.beginTransaction()
        result <- {
          attempt {
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
    def as[S <: SqlVal] : S =
      self.asInstanceOf[S]

    def asNullable[S <: SqlVal] : Option[S] =
      self match {
        case SqlVal.NULL(_) => None
        case _ => Some(self.as[S])
      }
  }

  implicit class OptionSqlValPML(val self: Option[SqlVal]) extends AnyVal {
    def orSqlNull : SqlVal =
      self match {
        case Some(v) => v
          // todo: either this to OptionSqlXXXPML to preserve sql type (sql type required by JDBC PreparedStatement.setNull
        case None => SqlVal.NULL(null)
      }
  }

  implicit object LiftService_SqlDriver$ extends LiftService[SqlDriver] {
    override def apply[E[_], F[_]](
      s: SqlDriver[E]
    )(implicit
      E: Exec[E],
      F: Exec[F],
      liftExec: LiftExec[E, F]
    ): SqlDriver[F] = {
      import SqlDriver._
      import Context.InTransaction
      new SqlDriver[F] {
        override def beginTransaction(): F[InTransaction] =
          liftExec(s.beginTransaction())
        override def executePreparedUpdate(preparedStatementId: PreparedStatementId)(rows: SqlRow*): F[Int] =
          liftExec(s.executePreparedUpdate(preparedStatementId)(rows:_*))
        override def nextCursor(cursorId: CursorId): F[Cursor] =
          liftExec(s.nextCursor(cursorId))
        override def executePreparedQuery(preparedStatementId: PreparedStatementId)(rows: SqlRow*): F[Cursor] =
          liftExec(s.executePreparedQuery(preparedStatementId)(rows:_*))
        override def getCursorMetadata(cursorId: CursorId): F[CursorMetadata] =
          liftExec(s.getCursorMetadata(cursorId))
        override def closeCursor(cursorId: CursorId): F[Unit] =
          liftExec(s.closeCursor(cursorId))
        override def rollback()(implicit context: InTransaction): F[Unit] =
          liftExec(s.rollback())
        override def executeQuery(statement: String)(implicit context: Context): F[Cursor] =
          liftExec(s.executeQuery(statement))
        override def executeUpdate(statement: String)(implicit context: Context): F[Int] =
          liftExec(s.executeUpdate(statement))
        override def prepare(statement: String)(implicit context: Context): F[PreparedStatementId] =
          liftExec(s.prepare(statement))
        override def commit()(implicit context: InTransaction): F[Unit] =
          liftExec(s.commit())
      }
    }
  }
}
