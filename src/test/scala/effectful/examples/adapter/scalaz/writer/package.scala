package effectful.examples.adapter.scalaz

import effectful.Exec
import effectful.cats.Monad
import effectful.impl.StdPar

import scalaz._
import Scalaz._

package object writer {
  type LogWriter[A] = scalaz.Writer[List[LogEntry],A]
  object LogWriter {
    def apply[A](a: A) : LogWriter[A] =
      scalaz.Writer(Nil, a)
    def apply[A](entries: List[LogEntry], a: A) : LogWriter[A] =
      scalaz.Writer(entries, a)
  }


  implicit object exec_LogWriter extends
    Exec.ImmediateNoCaptureExceptions[LogWriter] with
    StdPar[LogWriter]
  {
    override implicit val E: Exec[LogWriter] = this

    // todo: writer doesn't capture effects
    def capture[A](a: => A) = ???

    override def map[A, B](m: LogWriter[A])(f: (A) => B): LogWriter[B] =
      m.map(f)
    override def flatMap[A, B](m: LogWriter[A])(f: (A) => LogWriter[B]): LogWriter[B] =
      m.flatMap(f)
    override def widen[A, AA >: A](ea: LogWriter[A]): LogWriter[AA] =
      ea.asInstanceOf[LogWriter[AA]]
    override def pure[A](a: A): LogWriter[A] =
      LogWriter(a)
    def foreach[A,U](ea: LogWriter[A])(f: (A) => U) =
      f(ea.run._2)

    override def flatSequence[M[_], A, B](ta: LogWriter[A])(f: (A) => M[LogWriter[B]])(implicit M: Monad[M]): M[LogWriter[B]] = {
      val (acc,a) = ta.run
      M.map(f(a))(_.<++:(acc))
    }
  }
}
