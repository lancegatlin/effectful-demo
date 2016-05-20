package effectful.examples.effects.logging

import scala.language.higherKinds
import effectful.EffectSystem
import scala.collection.generic.CanBuildFrom
import scalaz._, Scalaz._

package object writer {
  type LogWriter[A] = scalaz.Writer[List[LogEntry],A]
  object LogWriter {
    def apply[A](a: A) : LogWriter[A] =
      scalaz.Writer(Nil, a)
    def apply[A](entries: List[LogEntry], a: A) : LogWriter[A] =
      scalaz.Writer(entries, a)
  }

  implicit object EffectSystem_LogWriter extends EffectSystem[LogWriter] {
    override def map[A, B](m: LogWriter[A], f: (A) => B): LogWriter[B] =
      m.map(f)
    override def flatMap[A, B](m: LogWriter[A], f: (A) => LogWriter[B]): LogWriter[B] =
      m.flatMap(f)
    override def Try[A](_try: =>LogWriter[A])(_catch: PartialFunction[Throwable, LogWriter[A]]): LogWriter[A] =
      try { _try } catch _catch
    override def widen[A, AA >: A](ea: LogWriter[A]): LogWriter[AA] =
      ea.asInstanceOf[LogWriter[AA]]
//    override def sequence[F[AA] <: Traversable[AA], A](fea: F[LogWriter[A]])(implicit cbf: CanBuildFrom[Nothing, A, F[A]]): LogWriter[F[A]] =
//      fea.sequence
    override def apply[A](a: => A): LogWriter[A] =
      LogWriter(a)
  }
}
