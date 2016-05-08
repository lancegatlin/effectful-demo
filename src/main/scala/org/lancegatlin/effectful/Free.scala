package org.lancegatlin.effectful

import scala.language.higherKinds

trait Free[Effect[_],+A] {
  def map[B](f: A => B) : Free[Effect, B]
  def flatMap[B](f: A => Free[Effect,B]) : Free[Effect,B]
}

object Free {
  case class Effect[Effect[_],A](effect: Effect[A]) extends Free[Effect,A] {
    override def map[B](f: (A) => B): Free[Effect, B] =
      Map(this,f)
    override def flatMap[B](f: (A) => Free[Effect, B]): Free[Effect, B] =
      FlatMap(this,f)
  }
  case class Val[Effect[_],A](value: A) extends Free[Effect,A] {
    override def map[B](f: (A) => B): Free[Effect, B] =
      Val(f(value))
    override def flatMap[B](f: (A) => Free[Effect, B]): Free[Effect, B] =
      f(value)
  }
  case class Map[Effect[_],A,B](
    base: Free[Effect,A],
    map: A => B
  ) extends Free[Effect,B] {
    override def map[C](f: (B) => C): Free[Effect, C] =
      Map(base,map andThen f)
    override def flatMap[C](f: (B) => Free[Effect, C]): Free[Effect, C] =
      FlatMap(this, f)
  }
  case class FlatMap[Effect[_],A,B](
    base: Free[Effect,A],
    flatMap: A => Free[Effect,B]
  ) extends Free[Effect,B] {
    override def map[C](f: (B) => C): Free[Effect, C] =
      FlatMap(base, flatMap andThen(_.map(f)))
    override def flatMap[C](f: (B) => Free[Effect, C]): Free[Effect, C] =
      FlatMap(base, flatMap andThen(_.flatMap(f)))
  }
}
