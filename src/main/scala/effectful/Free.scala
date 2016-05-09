package effectful

import scala.language.higherKinds

trait Free[Cmd[_],+A] {
  def map[B](f: A => B) : Free[Cmd, B]
  def flatMap[B](f: A => Free[Cmd,B]) : Free[Cmd,B]
}

object Free {
  case class Cmd[Cmd[_],A](Cmd: Cmd[A]) extends Free[Cmd,A] {
    override def map[B](f: (A) => B): Free[Cmd, B] =
      Map(this,f)
    override def flatMap[B](f: (A) => Free[Cmd, B]): Free[Cmd, B] =
      FlatMap(this,f)
  }
  case class Val[Cmd[_],A](value: A) extends Free[Cmd,A] {
    override def map[B](f: (A) => B): Free[Cmd, B] =
      Val(f(value))
    override def flatMap[B](f: (A) => Free[Cmd, B]): Free[Cmd, B] =
      f(value)
  }
  case class Map[Cmd[_],A,B](
    base: Free[Cmd,A],
    map: A => B
  ) extends Free[Cmd,B] {
    override def map[C](f: (B) => C): Free[Cmd, C] =
      Map(base,map andThen f)
    override def flatMap[C](f: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(this, f)
  }
  case class FlatMap[Cmd[_],A,B](
    base: Free[Cmd,A],
    flatMap: A => Free[Cmd,B]
  ) extends Free[Cmd,B] {
    override def map[C](f: (B) => C): Free[Cmd, C] =
      FlatMap(base, flatMap andThen(_.map(f)))
    override def flatMap[C](f: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(base, flatMap andThen(_.flatMap(f)))
  }
}
