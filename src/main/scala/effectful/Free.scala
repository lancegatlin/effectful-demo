package effectful

import scala.language.higherKinds

trait Free[Cmd[_],A] {
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
    f: A => B
  ) extends Free[Cmd,B] {
    override def map[C](g: (B) => C): Free[Cmd, C] =
      Map(base,f andThen g)
    override def flatMap[C](g: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(this, g)
  }
  case class FlatMap[Cmd[_],A,B](
    base: Free[Cmd,A],
    f: A => Free[Cmd,B]
  ) extends Free[Cmd,B] {
    override def map[C](g: (B) => C): Free[Cmd, C] =
      FlatMap(base, f andThen(_.map(g)))
    override def flatMap[C](g: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(base, f andThen(_.flatMap(g)))
  }

  trait Interpreter[Cmd[_],E[_]] {
    implicit def E:EffectSystem[E]

    def apply[A](cmd: Cmd[A]) : E[A]

    def apply[A](free: Free[Cmd, A]): E[A] = {
      def loop[B]: Free[Cmd, B] => E[B] = {
        case Free.Val(a) => E(a)
        // Intellij erroneous errors here
        case Free.Cmd(cmd) => apply(cmd)
        case Free.Map(base, f) => loop(base).map(f)
        case Free.FlatMap(base, f) => loop(base).flatMap(inner => loop(f(inner)))
      }
      loop(free)
    }
  }
}
