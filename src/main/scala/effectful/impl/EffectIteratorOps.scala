package effectful.impl

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import effectful._
import effectful.cats.Monad

import scala.collection.generic.CanBuildFrom

object EffectIteratorOps {
  def collect[E[_],A,M[_]](ea: EffectIterator[E,A])(implicit cbf:CanBuildFrom[Nothing,A,M[A]]) : E[M[A]] = {
    import ea._
    import Monad.ops._

    val builder = cbf()
    def loop(): E[M[A]] = {
      next().flatMap {
        case Some(a) =>
          builder += a
          loop()
        case None =>
          E(builder.result())
      }
    }
    loop()
  }

  def empty[E[_],A](implicit E: Monad[E]) = {
    val _E = E
    new EffectIterator[E,A] {
      override implicit val E = _E
      val none : E[Option[A]] = E(None)
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

  case class Map[E[_],A,B](
    base: EffectIterator[E,A],
    f: A => B
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,B] {
    override def next(): E[Option[B]] = {
      import Monad.ops._
      base.next().map(_.map(f))
    }
  }

  case class FlatMap[E[_],A,B](
    base: EffectIterator[E,A],
    f: A => EffectIterator[E,B]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,B] {
    import Monad.ops._

    // Note: EffectIterator is not thread safe itself, however, E might be async so need to ensure
    // volatile for thread safety within next() below
    private[this] val current = new AtomicReference[Option[EffectIterator[E,B]]](None)
    override def next(): E[Option[B]] = {
      current.get match {
        case sib@Some(ib) =>
          for {
            optB <- ib.next()
            result <- optB match {
              case sb@Some(b) =>
                E(sb)
              case None =>
                current.compareAndSet(sib,None)
                next()
            }
          } yield result
        case None =>
          for {
            oa <- base.next()
            result <- oa match {
              case Some(a) =>
                val ib = f(a)
                current.compareAndSet(None,Some(ib))
                next()
              case None =>
                E(None)
            }
          } yield result
      }
    }
  }

  case class Append[E[_],A](
    first: EffectIterator[E,A],
    second: EffectIterator[E,A]
  )(implicit
    val E: Monad[E]
  ) extends EffectIterator[E,A] {
    import Monad.ops._

    private[this] val isFirstExhausted = new AtomicBoolean(false)
    override def next(): E[Option[A]] = {
      if(isFirstExhausted.get) {
        for {
          oa <- first.next()
          result <- oa match {
            case s@Some(_) => E(s)
            case None =>
              isFirstExhausted.set(true)
              next()
          }
        } yield result
      } else {
        second.next()
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
        E(Some(values.next()))
      } else {
        E(None)
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
