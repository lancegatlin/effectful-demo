package effectful.impl

import effectful._
import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

object EffectIteratorOps {
  def collect[E[_],A,M[_]](ea: EffectIterator[E,A])(implicit cbf:CanBuildFrom[Nothing,A,M[A]]) : E[M[A]] = {
    import ea._
    import Monad.ops._

    val builder = cbf()
    for {
      initial <- initialize()
      result <- {
        def loop(state: S): E[M[A]] = {
          next(state).flatMap {
            case Some((s,a)) =>
              builder += a
              loop(s)
            case None =>
              E.pure(builder.result())
          }
        }
        loop(initial)
      }
    } yield result
  }

  def empty[E[_],A](implicit E: Monad[E]) : EffectIterator[E,A] = {
    val _E = E
    new EffectIterator[E,A] {
      override implicit val E = _E
      type S = Unit
      val none : E[Option[(Unit,A)]] = E.pure(None)
      def next(s: S) = none
      def initialize() = E.pure(())
    }
  }

  def apply[E[_],SS,A](
    initialize: () => E[SS]
  )(
    next: SS => E[Option[(SS,A)]]
  )(implicit
    E: Monad[E]
  ) : EffectIterator[E,A] = {
    import Monad.ops._
    val _E = E
    val _next = next
    val _initialize = initialize
    new EffectIterator[E,A] {
      override implicit val E = _E
      type S = SS
      def initialize() = _initialize()
      def next(s: S) =  _next(s)
    }
  }

  def computed[E[_],A](a: A*)(implicit E: Monad[E]) : EffectIterator[E,A] =
    FromIterator(a.iterator)

//  def flatten[E[_],A](
//    eia: E[EffectIterator[E,A]]
//  )(implicit E: Monad[E]) : EffectIterator[E,A] =
//    Flatten[E,A](eia)

  case class Map[E[_],A,B](
    base: EffectIterator[E,A],
    f: A => B
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,B] {
    type S = base.S

    override def initialize(): E[S] = base.initialize()

    override def next(s: S): E[Option[(S,B)]] = {
      import Monad.ops._
      base.next(s).map (_.map { case (nextS,a) => (nextS,f(a)) })
    }
  }

//  case class FlatMap[E[_],A,B](
//    base: EffectIterator[E,A],
//    f: A => EffectIterator[E,B]
//  )(implicit
//    val E: Monad[E]
//  ) extends EffectIterator[E,B] {
//    import Monad.ops._
//
//    type S1 = base.S
//    case class Inner(
//      ib: EffectIterator[E,B]
//    )(
//      val s: ib.S
//    ) {
//      def next() : E[Option[(ib.S,B)]] =
//        ib.next(s)
//    }
//
//    case class S(
//      s1: S1,
//      optInner: Option[Inner]
//    )
//
//    override def initialize(): E[S] =
//      base.initialize().map(S(_,None))
//
//    override def next(s: S): E[Option[(S,B)]] = {
//      s match {
//        case S(s1,None) =>
//          base.next(s1).flatMap {
//            case Some((nextS1,a)) =>
//              val ib = f(a)
//              for {
//                state <- ib.initialize()
//                result <- next(S(s1,Some(Inner(ib)(state))))
//              } yield result
//            case None =>
//              E.pure(None)
//          }
//        case S(s1,Some(inner@Inner(ib))) =>
//            inner.next().flatMap {
//              case Some((nextInnerS,b)) =>
//                E.pure(Some((S(s1,Some(Inner(ib)(nextInnerS))),b)))
//              case None =>
//                next(S(s1,None))
//            }
//      }
//    }
//  }

  case class Append[E[_],A](
    first: EffectIterator[E,A],
    second: EffectIterator[E,A]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,A] {
    import Monad.ops._
    type S1 = first.S
    type S2 = second.S
    type S = (S1,Option[S2])

    override def initialize(): E[(S1, Option[S2])] =
      first.initialize().map((_,None))

    override def next(s: S): E[Option[(S,A)]] =
      s match {
        case (s1,None) =>
          first.next(s1).flatMap {
            case None =>
              for {
                s2 <- second.initialize()
                result <- next((s1,Some(s2)))
              } yield result
            case Some((nextS1,a)) =>
              E.pure(Some(((nextS1,None),a)))
          }
        case (s1,Some(s2)) =>
          second.next(s2).map {
            case None => None
            case Some((nextS2,a)) =>
              Some(((s1,Some(nextS2)),a))
          }
      }

  }

  case class FromIterator[E[_],A](
    values: Iterator[A]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,A] {
    type S = Iterator[A]
    def initialize() = E.pure(values)
    override def next(s: S): E[Option[(S,A)]] =
      if(s.hasNext) {
        E.pure(Some((s,s.next())))
      } else {
        E.pure(None)
      }
  }

//  case class Flatten[E[_],A](
//    eia: E[EffectIterator[E,A]]
//  )(implicit
//    val E: Monad[E]
//  ) extends EffectIterator[E,A] {
//    import Monad.ops._
//
//    type S = eia
//
//    def next(s: Flatten.this.type) = ???
//
//    def initialize() = eia.flatMap(_.initialize())
//  }
}
