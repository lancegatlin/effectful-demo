package effectful.impl

import effectful._
import effectful.aspects.Par

import scala.collection.generic.CanBuildFrom

trait IdPar extends Par[Id] {
  override def par[A, B](ea: => Id[A], eb: => Id[B]): (A, B) =
    (ea,eb)

  override def par[A, B, C](ea: => Id[A], eb: => Id[B], ec: => Id[C]): (A, B, C) =
    (ea,eb,ec)

  override def par[A, B, C, D](ea: => Id[A], eb: => Id[B], ec: => Id[C], ed: => Id[D]): (A, B, C, D) =
    (ea,eb,ec,ed)

  override def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => Id[B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Id[M[B]] =
    items.map(f)(scala.collection.breakOut)

  override def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => Id[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Id[M[B]] =
    items.flatMap(f)(scala.collection.breakOut)

  override def parMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => Id[B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Id[M[B]] =
    items.map(f)(scala.collection.breakOut)

  override def parFlatMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => Id[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Id[M[B]] =
    items.flatMap(f)(scala.collection.breakOut)
}
