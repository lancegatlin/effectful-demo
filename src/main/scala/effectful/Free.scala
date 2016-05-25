package effectful


/**
  * A free monad
  *
  * Note: provided to keep this lib independent of scalaz. Feel free to use
  * scalaz.Free instead
  *
  * @tparam Cmd a type of command
  * @tparam A monad type
  */
// todo: choose either: 1) encode effect system requirements in Free as a type parameter OR
// todo: 2) add commands for each effect system (Par,Exceptions,etc)
trait Free[Cmd[_],A] {
  def map[B](f: A => B) : Free[Cmd, B]
  def flatMap[B](f: A => Free[Cmd,B]) : Free[Cmd,B]
  def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd,Cmd2]) : Free[Cmd2,A]
  def run[E[_]](i: Interpreter[Cmd,E]) : E[A]
  def widen[AA >: A] : Free[Cmd,AA]
}

object Free {

  case class Command[Cmd[_],A](cmd: Cmd[A]) extends Free[Cmd,A] {
    override def map[B](f: (A) => B): Free[Cmd, B] =
      Map(this,f)
    override def flatMap[B](f: (A) => Free[Cmd, B]): Free[Cmd, B] =
      FlatMap(this,f)
    def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd,Cmd2]) : Command[Cmd2,A] =
      Command(liftCmd(cmd))
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i(cmd)
    override def widen[AA >: A]: Command[Cmd, AA] = this.asInstanceOf
  }
  case class Val[Cmd[_],A](value: A) extends Free[Cmd,A] {
    override def map[B](f: (A) => B): Free[Cmd, B] =
      Val(f(value))
    override def flatMap[B](f: (A) => Free[Cmd, B]): Free[Cmd, B] =
      f(value)
    override def liftCmd[Cmd2[_]](implicit liftCmd: LiftCmd[Cmd, Cmd2]): Val[Cmd2, A] =
      this.asInstanceOf[Val[Cmd2,A]]
    override def run[E[_]](i: Interpreter[Cmd, E]): E[A] =
      i.E(value)
    override def widen[AA >: A]: Val[Cmd, AA] = this.asInstanceOf
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
    override def widen[BB >: B]: Map[Cmd,A,BB] = this.asInstanceOf
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
    override def widen[BB >: B]: FlatMap[Cmd,A,BB] = this.asInstanceOf
  }
}
