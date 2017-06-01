package effectful.examples.adapter

import cats._
import cats.data.WriterT
import cats.implicits._
import effectful.augments.Capture

package object writer {
  type LogWriter[A] = WriterT[Id,List[LogEntry],A]
  object LogWriter {
    def apply[A](a: A) : LogWriter[A] =
      WriterT.value(a)
    def apply[A](entries: List[LogEntry], a: A) : LogWriter[A] =
      WriterT.put(a)(entries)

  }

  // todo: why doesn't cats have this?
  implicit val traverse_LogWriter = new Traverse[LogWriter] {
    def traverse[G[_], A, B](fa: LogWriter[A])(f: (A) => G[B])(implicit G: Applicative[G]) : G[LogWriter[B]] =
      f(fa.run._2).map(LogWriter(fa.run._1,_))

    def foldLeft[A, B](fa: LogWriter[A], b: B)(f: (B, A) => B) =
      f(b,fa.run._2)

    def foldRight[A, B](fa: LogWriter[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]) =
      f(fa.run._2,lb)
  }

  implicit val capture_LogWriter = Capture.fromApplicative[LogWriter]
}
