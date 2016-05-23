package effectful.examples.effects.par.impl

import effectful._
import effectful.examples.effects.par.ParSystem

import scala.collection.generic.CanBuildFrom

class FakeParSystem[E[_]](implicit E:EffectSystem[E]) extends ParSystem[E] {
  def par[A, B](ea: => E[A], eb: => E[B]) =
    for {
      a <- ea
      b <- eb
    } yield (a,b)

  def par[A, B, C](ea: => E[A], eb: => E[B], ec: => E[C]) =
    for {
      a <- ea
      b <- eb
      c <- ec
    } yield (a,b,c)

  def par[A, B, C, D](ea: => E[A], eb: => E[B], ec: => E[C], ed: => E[D]) =
    for {
      a <- ea
      b <- eb
      c <- ec
      d <- ed
    } yield (a,b,c,d)

  def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[B])(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) =
  // todo:
    ???
    //items.map(f).sequence(implicitly,cbf)

  def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) =
  // todo:
    ???
//    items.map(f)(scala.collection.breakOut).sequence.map(_.flatten)
}
