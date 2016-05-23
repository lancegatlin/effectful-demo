package effectful.examples.effects.logging

import effectful.EffectSystem

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


  implicit object EffectSystem_LogWriter extends EffectSystem.Immediate[LogWriter] with EffectSystem.NoExceptionCapture[LogWriter] {
    override def map[A, B](m: LogWriter[A])(f: (A) => B): LogWriter[B] =
      m.map(f)
    override def flatMap[A, B](m: LogWriter[A])(f: (A) => LogWriter[B]): LogWriter[B] =
      m.flatMap(f)
    override def widen[A, AA >: A](ea: LogWriter[A]): LogWriter[AA] =
      ea.asInstanceOf[LogWriter[AA]]
    override def apply[A](a: => A): LogWriter[A] =
      LogWriter(a)
    def foreach[A,U](ea: LogWriter[A])(f: (A) => U) =
      f(ea.run._2)
    def flatSequence[F[_], A, B](ea: LogWriter[A])(f: (A) => F[LogWriter[B]])(implicit F: EffectSystem[F]) : F[LogWriter[B]] = {
      val (acc,a) = ea.run
      F.map(f(a))(_.<++:(acc))
    }

  }
}
