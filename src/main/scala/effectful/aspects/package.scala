package effectful

import scala.collection.generic.CanBuildFrom

package object aspects {
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

}
