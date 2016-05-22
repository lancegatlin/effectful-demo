package effectful.examples.effects.par

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

trait ParSystem[E[_]] {
  def par[A,B](ea: =>E[A],eb: =>E[B]) : E[(A,B)]
  def par[A,B,C](ea: =>E[A],eb: =>E[B],ec: =>E[C]) : E[(A,B,C)]
  def par[A,B,C,D](ea: =>E[A],eb: =>E[B],ec: =>E[C],ed: =>E[D]) : E[(A,B,C,D)]
  // todo: gen more

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
}