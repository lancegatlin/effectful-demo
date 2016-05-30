package effectful

import effectful.cats.Capture

import scala.collection.generic.CanBuildFrom
import scala.concurrent.duration.FiniteDuration

package object aspects {
  implicit object liftService_Delay extends LiftService[Delay] {
    override def apply[E[_], F[_]](
      s: Delay[E]
    )(implicit
      E: Capture[E],
      F: Capture[F],
      liftCapture: LiftCapture[E, F]
    ): Delay[F] =
      new Delay[F] {
        override def delay(duration: FiniteDuration): F[Unit] =
          liftCapture(s.delay(duration))
      }
  }
  
  implicit class ParHelper[M[AA] <: Seq[AA],A,E[_]](
    val self: M[A]
  ) extends AnyVal {
    def map[B](
      f: A => E[B]
    )(implicit
      p: Par[E],
      cbf: CanBuildFrom[Nothing,B,M[B]]
    ) : E[M[B]] =
      p.parMap(self)(f)
    def flatMap[B](
      f: A => E[Traversable[B]]
    )(implicit
      p: Par[E],
      cbf: CanBuildFrom[Nothing,B,M[B]]
    ) : E[M[B]] =
      p.parFlatMap(self)(f)
  }

  implicit class SeqPML[M[AA] <: Seq[AA],A](val self: M[A]) extends AnyVal {
    def par[E[_]] = new ParHelper[M,A,E](self)
  }

  implicit class EffectSystemPML[E[_]](val self: Exec[E]) extends AnyVal {
    def par[A,B](ea: =>E[A],eb: =>E[B])(implicit p:Par[E]) : E[(A,B)] =
      p.par(ea,eb)
    def par[A,B,C](ea: =>E[A],eb: =>E[B],ec: =>E[C])(implicit p:Par[E]) : E[(A,B,C)] =
      p.par(ea,eb,ec)
    def par[A,B,C,D](ea: =>E[A],eb: =>E[B],ec: =>E[C],ed: =>E[D])(implicit p:Par[E]) : E[(A,B,C,D)] =
      p.par(ea,eb,ec,ed)
  }
  
}
