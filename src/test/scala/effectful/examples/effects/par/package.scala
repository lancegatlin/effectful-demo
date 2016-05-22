package effectful.examples.effects

import scala.language.higherKinds
import effectful.EffectSystem

import scala.collection.generic.CanBuildFrom

package object par {
  implicit class ParHelper[M[AA] <: Seq[AA],A,E[_]](
    val self: M[A]
  ) extends AnyVal {
    def map[B](
      f: A => E[B]
    )(implicit
      p: ParSystem[E],
      cbf: CanBuildFrom[Nothing,B,M[B]]
    ) : E[M[B]] =
      p.parMap(self)(f)
    def flatMap[B](
      f: A => E[Traversable[B]]
    )(implicit
      p: ParSystem[E],
      cbf: CanBuildFrom[Nothing,B,M[B]]
    ) : E[M[B]] =
      p.parFlatMap(self)(f)
  }

  implicit class SeqPML[M[AA] <: Seq[AA],A](val self: M[A]) extends AnyVal {
    def par[E[_]] = new ParHelper[M,A,E](self)
  }

  implicit class EffectSystemPML[E[_]](val self: EffectSystem[E]) extends AnyVal {
    def par[A,B](ea: =>E[A],eb: =>E[B])(implicit p:ParSystem[E]) : E[(A,B)] =
      p.par(ea,eb)
    def par[A,B,C](ea: =>E[A],eb: =>E[B],ec: =>E[C])(implicit p:ParSystem[E]) : E[(A,B,C)] =
      p.par(ea,eb,ec)
    def par[A,B,C,D](ea: =>E[A],eb: =>E[B],ec: =>E[C],ed: =>E[D])(implicit p:ParSystem[E]) : E[(A,B,C,D)] =
      p.par(ea,eb,ec,ed)
  }
}
