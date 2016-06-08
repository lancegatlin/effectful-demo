package effectful.impl

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import effectful._
import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

object EffectIteratorOps {
  def collect[E[_],S,A,M[_]](ea: EffectIterator[E,S,A])(implicit cbf:CanBuildFrom[Nothing,A,M[A]]) : E[M[A]] = {
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
      val none : E[Option[A]] = E.pure(None)
      override def next(): E[Option[A]] = none
    }
  }

  def apply[E[_], A](
    f: () => E[Option[A]]
  )(implicit
    E: Monad[E]
  ) : EffectIterator[E,A] = {
    val _E = E
    new EffectIterator[E,A] {
      override implicit val E = _E
      override def next() = f()
    }
  }

  def computed[E[_],A](a: A*)(implicit E: Monad[E]) : EffectIterator[E,A] =
    FromIterator(a.iterator)

  def flatten[E[_],A](
    eia: E[EffectIterator[E,A]]
  )(implicit E: Monad[E]) : EffectIterator[E,A] =
    Flatten[E,A](eia)

  case class Map[E[_],S,A,B](
    base: EffectIterator[E,S,A],
    f: A => B
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,S,B] {

    override def initialize(): E[S] = base.initialize()

    override def next(s: S): E[Option[(S,B)]] = {
      import Monad.ops._
      base.next(s).map (_.map { case (nextS,a) => (nextS,f(a)) })
    }
  }

//  case class FlatMap[E[_],S,A,B](
//    base: EffectIterator[E,S,A],
//    f: A => EffectIterator[E,S,B]
//  )(implicit
//    val E: Monad[E]
//  ) extends EffectIterator[E,S,B] {
//    import Monad.ops._
//
//
////    // Note: EffectIterator is not thread safe itself, however, E might be async so need to ensure
////    // volatile for thread safety within next() below
////    private[this] val current = new AtomicReference[Option[EffectIterator[E,B]]](None)
////    override def next(): E[Option[B]] = {
////      current.get match {
////        case sib@Some(ib) =>
////          for {
////            optB <- ib.next()
////            result <- optB match {
////              case sb@Some(b) =>
////                E.pure(sb)
////              case None =>
////                current.compareAndSet(sib,None)
////                next()
////            }
////          } yield result
////        case None =>
////          for {
////            oa <- base.next()
////            result <- oa match {
////              case Some(a) =>
////                val ib = f(a)
////                current.compareAndSet(None,Some(ib))
////                next()
////              case None =>
////                E.pure(None)
////            }
////          } yield result
////      }
//    override def initialize(): E[S] = base.initialize()
//
//    override def next(s: S): E[Option[B]] = {
//      base.next(s).flatMap {
//        case Some(a) =>
//          f(a)
//        case None => None
//      }
//    }
//  }

  case class Append[E[_],S1,S2,A](
    first: EffectIterator[E,S1,A],
    second: EffectIterator[E,S2,A]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,(S1,Option[S2]),A] {
    import Monad.ops._
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
    override def next(): E[Option[A]] =
      if(values.hasNext) {
        E.pure(Some(values.next()))
      } else {
        E.pure(None)
      }
  }

  case class Flatten[E[_],A](
    eia: E[EffectIterator[E,A]]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,A] {
    import Monad.ops._

    override def next(): E[Option[A]] =
      eia.flatMap(_.next())
  }
}
