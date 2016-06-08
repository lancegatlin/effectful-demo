package effectful

import effectful.cats.{Applicative, Monad}

import scala.collection.generic.CanBuildFrom

/**
  * An iterator that encapsulates the logic of fetching input from
  * an effectful service until it is exhausted.
  *
  * Note: like a standard Scala iterator, EffectIterator is not
  * thread-safe itself, though all code in EffectIterator is
  * thread-safe when retrieving data from underlying effectful
  * service(s).
  *
  * @tparam E effect system's monad
  * @tparam A input type
  */
trait EffectIterator[E[_],S,A] { self =>
  implicit val E:Monad[E]

  def map[B](f: A => B) : EffectIterator[E,S,B] =
    impl.EffectIteratorOps.Map(this,f)

//  def flatMap[B](f: A => EffectIterator[E,S,B]) : EffectIterator[E,S,B] =
//    impl.EffectIteratorOps.FlatMap(this,f)
//


  def initialize() :  E[S]

  /**
    * Attempt to retrieve the next data item
    *
    * @return Some(data) or None if exhausted
    */
  def next(s: S): E[Option[(S,A)]]

  def headOption() : E[Option[A]] =
    for {
      state <- initialize()
      optSA:Option[(S,A)] <- next(state)
    } yield optSA.map(_._2)

  /**
    * @return a collection of all data items retrieved serially until exhausted
    */
  def collect[M[_]]()(implicit cbf:CanBuildFrom[Nothing,A,M[A]]) : E[M[A]] =
    impl.EffectIteratorOps.collect[E,S,A,M](this)

  /** @return a new EffectIterator that returns the data items from this iterator
    *         and after exhausted all data items from other until exhausted */
  def ++[S2](other: EffectIterator[E,S2,A]) : EffectIterator[E,_,A] =
    impl.EffectIteratorOps.Append(this,other)
}

object EffectIterator {
  def empty[E[_],A](implicit E: Monad[E]) : EffectIterator[E,A] =
    impl.EffectIteratorOps.empty[E,A]

  def apply[E[_], A](
    f: () => E[Option[A]]
  )(implicit
    E: Monad[E]
  ) : EffectIterator[E,A] =
    impl.EffectIteratorOps.apply(f)

  def computed[E[_],A](a: A*)(implicit E: Monad[E]) : EffectIterator[E,A] =
    impl.EffectIteratorOps.computed(a:_*)

  def flatten[E[_],A](
    eia: E[EffectIterator[E,A]]
  )(implicit E: Monad[E]) : EffectIterator[E,A] =
    impl.EffectIteratorOps.flatten[E,A](eia)
}