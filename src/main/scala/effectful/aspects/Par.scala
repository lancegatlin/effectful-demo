package effectful.aspects

import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

trait Par[E[_]] {

  def par[A,B](ea: =>E[A],eb: =>E[B]) : E[(A,B)]
  def par[A,B,C](ea: =>E[A],eb: =>E[B],ec: =>E[C]) : E[(A,B,C)]
  def par[A,B,C,D](ea: =>E[A],eb: =>E[B],ec: =>E[C],ed: =>E[D]) : E[(A,B,C,D)]
  // todo: gen more

  // todo: b/c of free monad interpreter this will prob have to be concrete Seq
  def parMap[M[AA] <: Seq[AA],A,B](
    items: M[A]
  )(
    f: A => E[B]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) : E[M[B]]

  def parFlatMap[M[AA] <: Seq[AA],A,B](
    items: M[A]
  )(
    f: A => E[Traversable[B]]
  )(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) : E[M[B]]

  def parMapUnordered[M[AA] <: Traversable[AA],A,B](
    items: M[A]
  )(
    f: A => E[B]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) : E[M[B]]

  def parFlatMapUnordered[M[AA] <: Traversable[AA],A,B](
    items: M[A]
  )(
    f: A => E[Traversable[B]]
  )(implicit cbf: CanBuildFrom[Nothing,B,M[B]]) : E[M[B]]
}
