package effectful.aspects

import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

trait CompositePar[F[_],G[_]] extends Par[({ type FG[A] = F[G[A]]})#FG] {
  import Monad.ops._

  val P:Par[F]
  implicit val F:Monad[F]
  implicit val G:Monad[G]

  def par[A, B](fga: => F[G[A]], fgb: => F[G[B]]) = {
    for {
      tuple <- P.par(fga,fgb)
      (ga,gb) = tuple
    } yield {
      for {
        a <- ga
        b <- gb
      } yield (a,b)
    }
  }

  def par[A, B, C](fga: => F[G[A]], fgb: => F[G[B]], fgc: => F[G[C]]) = {
    for {
      tuple <- P.par(fga,fgb,fgc)
      (ga,gb,gc) = tuple
    } yield {
      for {
        a <- ga
        b <- gb
        c <- gc
      } yield (a,b,c)
    }
  }

  def par[A, B, C, D](fga: => F[G[A]], fgb: => F[G[B]], fgc: => F[G[C]], fgd: => F[G[D]]) = {
    for {
      tuple <- P.par(fga,fgb,fgc,fgd)
      (ga,gb,gc,gd) = tuple
    } yield {
      for {
        a <- ga
        b <- gb
        c <- gc
        d <- gd
      } yield (a,b,c,d)
    }
  }

  // todo:
  def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => F[G[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) = ???

  def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => F[G[Traversable[B]]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) = ???

  def parMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => F[G[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) = ???

  def parFlatMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => F[G[Traversable[B]]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) = ???

}

object CompositePar {
  implicit def apply[F[_],G[_]](implicit
    P:Par[F],
    F:Monad[F],
    G:Monad[G]
  ) = {
    val _P = P
    val _F = F
    val _G = G
    new CompositePar[F,G] {
      override val P = _P
      override val G = _G
      override val F = _F
    }
  }
}