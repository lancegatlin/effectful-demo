package effectful.free

import scala.collection.generic.CanBuildFrom
import scala.concurrent.duration.FiniteDuration

/**
  * A free monad for an effect system
  *
  * @tparam Cmd a type of command
  * @tparam A monad type
  */
sealed trait Free[Cmd[_],A] {
  def map[B](f: A => B) : Free[Cmd, B] =
    Free.Map(this,f)
  def flatMap[B](f: A => Free[Cmd,B]) : Free[Cmd,B] =
    Free.FlatMap(this,f)
  def widen[AA >: A] : Free[Cmd,AA] =
    this.asInstanceOf

  def run[E[_]](i: Interpreter[Cmd,E]) : E[A]

  def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd,Cmd2]) : Free[Cmd2,A]
}

object Free {
  def apply[Cmd[_],A](a: A) : Apply[Cmd,A] = Apply(a)

  case class Command[Cmd[_],A](cmd: Cmd[A]) extends Free[Cmd,A] {
    def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd,Cmd2]) : Command[Cmd2,A] =
      Command(liftCmd(cmd))
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i(cmd)
  }
  // todo: should this be lazy? () => A
  case class Apply[Cmd[_],A](value: A) extends Free[Cmd,A] {
    override def map[B](f: (A) => B): Free[Cmd, B] =
      Apply(f(value))
    override def flatMap[B](f: (A) => Free[Cmd, B]): Free[Cmd, B] =
      f(value)
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Apply[Cmd2, A] =
      this.asInstanceOf[Apply[Cmd2,A]]
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i.E(value)
  }
  case class Map[Cmd[_],A,B](
    base: Free[Cmd,A],
    f: A => B
  ) extends Free[Cmd,B] {
    override def map[C](g: (B) => C): Free[Cmd, C] =
      Map(base,f andThen g)
    override def flatMap[C](g: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(this, g)
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Map[Cmd2,A,B] =
      Map(base.liftCmd,f)
    override def run[E[_]](i: Interpreter[Cmd, E]): E[B] =
      i.E.map(base.run(i))(f)
  }
  case class FlatMap[Cmd[_],A,B](
    base: Free[Cmd,A],
    f: A => Free[Cmd,B]
  ) extends Free[Cmd,B] {
    override def map[C](g: (B) => C): Free[Cmd, C] =
      FlatMap(base, f andThen(_.map(g)))
    override def flatMap[C](g: (B) => Free[Cmd, C]): Free[Cmd, C] =
      FlatMap(base, f andThen(_.flatMap(g)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): FlatMap[Cmd2,A,B] =
      FlatMap(base.liftCmd,f.andThen(_.liftCmd))
    override def run[E[_]](i: Interpreter[Cmd, E]): E[B] =
      i.E.flatMap(base.run(i))(inner => f(inner).run(i))
  }

  // Exceptions aspect
  // Note: since Free is already lazy no need for () => E[A] pattern here
  case class Try[Cmd[_],A](
    _try: Free[Cmd,A],
    _catch: PartialFunction[Throwable,Free[Cmd,A]]
  ) extends Free[Cmd,A] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i.E.attempt(_try.run(i))(_catch.andThen(_.run(i)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, A] =
      Try(
        _try.liftCmd,
        _catch.andThen(_.liftCmd)
      )
  }
  case class TryFinally[Cmd[_],A,U](
    _try: Free[Cmd,A],
    _catch: PartialFunction[Throwable,Free[Cmd,A]],
    _finally: Free[Cmd,U]
  ) extends Free[Cmd,A] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i.E.attemptFinally(_try.run(i))(_catch.andThen(_.run(i)))(_finally.run(i))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, A] =
      TryFinally(
        _try.liftCmd,
        _catch.andThen(_.liftCmd),
        _finally.liftCmd
      )
  }
  // Note: Success just uses Apply (unless it gets made lazy)
  case class Failure[Cmd[_]](t: Throwable) extends Free[Cmd,Nothing] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[Nothing] =
      i.E.failure(t)
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, Nothing] =
      this.asInstanceOf
  }

  // Delay aspect
  case class Delay[Cmd[_]](
    duration: FiniteDuration
  ) extends Free[Cmd,Unit] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[Unit] =
      i.E.delay(duration)
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, Unit] =
      this.asInstanceOf
  }

  // Par aspect
  case class Par2[Cmd[_],A,B](
    fa: Free[Cmd,A],
    fb: Free[Cmd,B]
  ) extends Free[Cmd,(A,B)] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[(A, B)] =
      i.E.par(fa.run(i),fb.run(i))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, (A, B)] =
      Par2(fa.liftCmd,fb.liftCmd)
  }
  case class Par3[Cmd[_],A,B,C](
    fa: Free[Cmd,A],
    fb: Free[Cmd,B],
    fc: Free[Cmd,C]
  ) extends Free[Cmd,(A,B,C)] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[(A,B,C)] =
      i.E.par(fa.run(i),fb.run(i),fc.run(i))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, (A,B,C)] =
      Par3(fa.liftCmd,fb.liftCmd,fc.liftCmd)
  }
  case class Par4[Cmd[_],A,B,C,D](
    fa: Free[Cmd,A],
    fb: Free[Cmd,B],
    fc: Free[Cmd,C],
    fd: Free[Cmd,D]
  ) extends Free[Cmd,(A,B,C,D)] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[(A,B,C,D)] =
      i.E.par(fa.run(i),fb.run(i),fc.run(i),fd.run(i))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, (A,B,C,D)] =
      Par4(fa.liftCmd,fb.liftCmd,fc.liftCmd,fd.liftCmd)
  }

  case class ParMap[Cmd[_],M[AA] <: Seq[AA],A,B](
    items: M[A],
    f: A => Free[Cmd,B]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) extends Free[Cmd,M[B]] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[M[B]] =
      i.E.parMap[M,A,B](items)(f.andThen(_.run(i)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, M[B]] =
      ParMap(items, f.andThen(_.liftCmd))
  }
  case class ParFlatMap[Cmd[_],M[AA] <: Seq[AA],A,B](
    items: M[A],
    f: A => Free[Cmd,Traversable[B]]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) extends Free[Cmd,M[B]] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[M[B]] =
      i.E.parFlatMap[M,A,B](items)(f.andThen(_.run(i)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, M[B]] =
      ParFlatMap(items, f.andThen(_.liftCmd))
  }
  case class ParMapUnordered[Cmd[_],M[AA] <: Traversable[AA],A,B](
    items: M[A],
    f: A => Free[Cmd,B]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) extends Free[Cmd,M[B]] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[M[B]] =
      i.E.parMapUnordered[M,A,B](items)(f.andThen(_.run(i)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, M[B]] =
      ParMapUnordered(items, f.andThen(_.liftCmd))
  }
  case class ParFlatMapUnordered[Cmd[_],M[AA] <: Traversable[AA],A,B](
    items: M[A],
    f: A => Free[Cmd,Traversable[B]]
  )(implicit
    cbf: CanBuildFrom[Nothing,B,M[B]]
  ) extends Free[Cmd,M[B]] {
    override def run[E[_]](i: Interpreter[Cmd, E]): E[M[B]] =
      i.E.parFlatMapUnordered[M,A,B](items)(f.andThen(_.run(i)))
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Free[Cmd2, M[B]] =
      ParFlatMapUnordered(items, f.andThen(_.liftCmd))
  }

}
