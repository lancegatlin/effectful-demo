package effectful.examples.effects

import effectful.{EffectIterator, LiftService}
import effectful.augments._
import cats._
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

    def iterator(fetchCursor: () => E[SqlDriver.InitialCursor])(implicit E:Monad[E]) : EffectIterator[E,SqlRow]= {
      import Monad.ops._

      EffectIterator.apply[E,InitialCursor,SqlRow](fetchCursor) {
        case InitialCursor.Empty =>
          E.pure(None)
        case c@InitialCursor.NonEmpty(cursorId) =>
          self.nextCursor(cursorId).map {
            case Cursor.Row(_,_,row) =>
              Some((c,row))
            case Cursor.Empty =>
              None
          }
      }
    }

    def autoCommit[A](
      f: Context.AutoCommit.type => E[A]
    ) : E[A] = f(Context.AutoCommit)

    def inTransaction[A](
      f: Context.InTransaction => E[A]
    )(implicit
      E:Monad[E],
      X:Exceptions[E]
    ) : E[A] = {
      import Monad.ops._

      for {
        transaction <- self.beginTransaction()
        result <- {
          E.attempt {
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

  implicit object LiftService_SqlDriver extends LiftService[SqlDriver] {
    override def apply[F[_], G[_]](
      s: SqlDriver[F]
    )(implicit
      X:CaptureTransform[F,G]
    ) = {
      import SqlDriver._
      import Context.InTransaction
      new SqlDriver[G] {
        override def beginTransaction() =
          X(s.beginTransaction())
        override def executePreparedUpdate(preparedStatementId: PreparedStatementId)(rows: SqlRow*) =
          X(s.executePreparedUpdate(preparedStatementId)(rows:_*))
        override def nextCursor(cursorId: CursorId) =
          X(s.nextCursor(cursorId))
        override def executePreparedQuery(preparedStatementId: PreparedStatementId)(rows: SqlRow*) =
          X(s.executePreparedQuery(preparedStatementId)(rows:_*))
        override def getCursorMetadata(cursorId: CursorId) =
          X(s.getCursorMetadata(cursorId))
        override def closeCursor(cursorId: CursorId) =
          X(s.closeCursor(cursorId))
        override def rollback()(implicit context: InTransaction) =
          X(s.rollback())
        override def executeQuery(statement: String)(implicit context: Context) =
          X(s.executeQuery(statement))
        override def executeUpdate(statement: String)(implicit context: Context) =
          X(s.executeUpdate(statement))
        override def prepare(statement: String)(implicit context: Context) =
          X(s.prepare(statement))
        override def commit()(implicit context: InTransaction) =
          X(s.commit())
      }
    }
  }
}
