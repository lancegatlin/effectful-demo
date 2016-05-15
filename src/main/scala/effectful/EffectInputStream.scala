package effectful

import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom

trait EffectInputStream[E[_],A] { self =>
  implicit val E:EffectSystem[E]

  def map[B](f: A => B) : EffectInputStream[E,B] =
    EffectInputStream.Map(this,f)

  def flatMap[B](f: A => EffectInputStream[E,B]) : EffectInputStream[E,B] =
    EffectInputStream.FlatMap(this,f)

  def next(): E[Option[A]]

  def headOption() : E[Option[A]] = next()

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
}

object EffectInputStream {
  def empty[E[_],A](implicit E:EffectSystem[E]) = {
    val _E = E
    new EffectInputStream[E,A] {
      override implicit val E = _E
      val none : E[Option[A]] = E(None)
      override def next(): E[Option[A]] = none
    }
  }
  def apply[E[_], A](
    f: () => E[Option[A]]
  )(implicit
    E: EffectSystem[E]
  ) : EffectInputStream[E,A] = {
    val _E = E
    new EffectInputStream[E,A] {
      override implicit val E = _E
      override def next() = f()
    }
  }
  case class Map[E[_],A,B](base: EffectInputStream[E,A], f: A => B) extends EffectInputStream[E,B] {
    override implicit val E = base.E
    override def next(): E[Option[B]] = base.next().map(_.map(f))
  }
  case class FlatMap[E[_],A,B](base: EffectInputStream[E,A], f: A => EffectInputStream[E,B]) extends EffectInputStream[E,B] {
    override implicit val E = base.E
    // todo: has to save the EffectInputStream retrieved by applying base to f until its exhausted
    override def next(): E[Option[B]] = ???
  }
}