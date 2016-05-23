package effectful

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import scala.collection.generic.CanBuildFrom

/**
  * An iterator that encapsulates the logic of fetching input from
  * an effectful service until it is exhausted.
  *
  * Note: like a standard Scala iterator, EffectIterator is not
  * thread-safe itself, though all code in EffectIterator is
  * thread-safe when retrieving data from underlying effectful
  * service.
  *
  * @tparam E effect system's monad
  * @tparam A input type
  */
trait EffectIterator[E[_],A] { self =>
  implicit val E:EffectSystem[E]

  def map[B](f: A => B) : EffectIterator[E,B] =
    EffectIterator.Map(this,f)

  def flatMap[B](f: A => EffectIterator[E,B]) : EffectIterator[E,B] =
    EffectIterator.FlatMap(this,f)

  /**
    * Attempt to retrieve the next data item
    * @return Some(data) or None if exhausted
    */
  def next(): E[Option[A]]

  def headOption() : E[Option[A]] = next()

  /**
    * @return a collection of all data items retrieved serially until exhausted
    */
  def collect[M[_]]()(implicit cbf:CanBuildFrom[Nothing,A,M[A]]) : E[M[A]] = {
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

  /** @return a new EffectIterator that returns the data items from this iterator
    *         and after exhausted all data items from other until exhausted */
  def ++(other: EffectIterator[E,A]) : EffectIterator[E,A] =
    EffectIterator.Append(this,other)
}

object EffectIterator {
  def empty[E[_],A](implicit E:EffectSystem[E]) = {
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
    E: EffectSystem[E]
  ) : EffectIterator[E,A] = {
    val _E = E
    new EffectIterator[E,A] {
      override implicit val E = _E
      override def next() = f()
    }
  }
  def computed[E[_],A](a: A*)(implicit E:EffectSystem[E]) : EffectIterator[E,A] =
    FromIterator(a.iterator)

  def flatten[E[_],A](
    eia: E[EffectIterator[E,A]]
  )(implicit E:EffectSystem[E]) : EffectIterator[E,A] =
    Flatten[E,A](eia)

  case class Map[E[_],A,B](
    base: EffectIterator[E,A],
    f: A => B
  )(implicit
    val E: EffectSystem[E]
  ) extends EffectIterator[E,B] {
    override def next(): E[Option[B]] = base.next().map(_.map(f))
  }

  case class FlatMap[E[_],A,B](
    base: EffectIterator[E,A],
    f: A => EffectIterator[E,B]
  )(implicit
    val E:EffectSystem[E]
  ) extends EffectIterator[E,B] {
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
    val E:EffectSystem[E]
  ) extends EffectIterator[E,A] {
    private[this] val isFirstExhausted = new AtomicBoolean(false)
    override def next(): E[Option[A]] =
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

  case class FromIterator[E[_],A](
    values: Iterator[A]
  )(implicit
    val E: EffectSystem[E]
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
    val E: EffectSystem[E]
  ) extends EffectIterator[E,A] {
    override def next(): E[Option[A]] =
      eia.flatMap(_.next())
  }
}