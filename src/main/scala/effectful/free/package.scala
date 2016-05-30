package effectful

import scala.collection.generic.CanBuildFrom
import scala.concurrent.duration.FiniteDuration

package object free {
  implicit def effectSystem_Free[Cmd[_]] = new Exec[({ type E[AA] = Free[Cmd,AA] })#E] {
    def map[A, B](m: Free[Cmd, A])(f: (A) => B) =
      m.map(f)
    def flatMap[A, B](m: Free[Cmd, A])(f: (A) => Free[Cmd, B]) =
      m.flatMap(f)
    def apply[A](a: => A) = 
      Free.Apply(a)
    def widen[A, AA >: A](ea: Free[Cmd, A]) = 
      ea.widen[AA]

    def attempt[A](_try: => Free[Cmd, A])(_catch: PartialFunction[Throwable, Free[Cmd, A]]) =
      Free.Try(_try,_catch)
    def attemptFinally[A, U](_try: => Free[Cmd, A])(_catch: PartialFunction[Throwable, Free[Cmd, A]])(_finally: => Free[Cmd, U]) =
      Free.TryFinally(_try,_catch,_finally)
    def failure(t: Throwable): Free[Cmd, Nothing] =
      Free.Failure(t)
    def success[A](a: A): Free[Cmd, A] =
      Free.Apply(a)

    def delay(duration: FiniteDuration): Free[Cmd, Unit] =
      Free.Delay(duration)

    def par[A, B](ea: => Free[Cmd, A], eb: => Free[Cmd, B]): Free[Cmd, (A, B)] =
      Free.Par2(ea,eb)
    def par[A, B, C](ea: => Free[Cmd, A], eb: => Free[Cmd, B], ec: => Free[Cmd, C]): Free[Cmd, (A, B, C)] =
      Free.Par3(ea,eb,ec)
    def par[A, B, C, D](ea: => Free[Cmd, A], eb: => Free[Cmd, B], ec: => Free[Cmd, C], ed: => Free[Cmd, D]): Free[Cmd, (A, B, C, D)] =
      Free.Par4(ea,eb,ec,ed)

    def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => Free[Cmd, B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Free[Cmd, M[B]] =
      Free.ParMap(items,f)

    def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => Free[Cmd, Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Free[Cmd, M[B]] =
      Free.ParFlatMap(items,f)

    def parMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => Free[Cmd, B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Free[Cmd, M[B]] =
      Free.ParMapUnordered(items,f)

    def parFlatMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => Free[Cmd, Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]): Free[Cmd, M[B]] =
      Free.ParFlatMapUnordered(items,f)
  }

  implicit def liftE_Free[Cmd1[_],Cmd2[_]](implicit
    liftCmd:LiftCmd[Cmd1,Cmd2]
  ) = new LiftExec[({ type F[AA] = Free[Cmd1,AA]})#F,({ type F[AA] = Free[Cmd2,AA]})#F] {
    override def apply[A](
      ea: => Free[Cmd1,A]
    )(implicit
      E: Exec[({ type F[AA] = Free[Cmd1,AA]})#F],
      F: Exec[({ type F[AA] = Free[Cmd2,AA]})#F]
    ): Free[Cmd2,A] =
      ea.liftCmd[Cmd2]
  }
}
