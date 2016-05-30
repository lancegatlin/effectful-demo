package effectful.impl

import effectful.aspects.Par
import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

trait StdPar[E[_]] extends Par[E] {
  import Monad.ops._

  implicit val E:Monad[E]

  // Note: invoke lazy outside for-comp in case E is async + eager

  def par[A, B](ea: => E[A], eb: => E[B]) = {
    val _ea = ea
    val _eb = eb
    for {
      a <- _ea
      b <- _eb
    } yield (a,b)
  }

  def par[A, B, C](ea: => E[A], eb: => E[B], ec: => E[C]) = {
    val _ea = ea
    val _eb = eb
    val _ec = ec
    for {
      a <- _ea
      b <- _eb
      c <- _ec
    } yield (a,b,c)
  }

  def par[A, B, C, D](ea: => E[A], eb: => E[B], ec: => E[C], ed: => E[D]) = {
    val _ea = ea
    val _eb = eb
    val _ec = ec
    val _ed = ed
    for {
      a <- _ea
      b <- _eb
      c <- _ec
      d <- _ed
    } yield (a,b,c,d)
  }

  def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[B])(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) =
  // todo:
    ???
    //items.map(f).sequence(implicitly,cbf)

  def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) =
  // todo:
    ???
//    items.map(f)(scala.collection.breakOut).sequence.map(_.flatten)
  override def parMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => E[B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): E[M[B]] = ???

  override def parFlatMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => E[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): E[M[B]] = ???
}
