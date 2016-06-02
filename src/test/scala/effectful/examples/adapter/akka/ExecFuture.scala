package effectful.examples.adapter.akka

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import effectful.Exec
import effectful.aspects.{Delay, Exceptions}
import effectful.cats.{Capture, Monad}
import effectful.impl.StdPar
import s_mach.concurrent._

object ExecFuture {
  def apply()(implicit
    ec: ExecutionContext,
    ses:ScheduledExecutionContext
  ) : Exec[Future] =
    new Exec[Future] with
        Capture[Future] with
        Monad[Future] with
        Exceptions[Future] with
        StdPar[Future] with
        Delay[Future] {
    implicit val E = this

    override def capture[A](a: => A) = Future(a)

    override def map[A, B](m: Future[A])(f: (A) => B): Future[B] =
      m.map(f)
    override def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] =
      m.flatMap(f)
    override def widen[A, AA >: A](ea: Future[A]): Future[AA] =
      ea
    override def pure[A](a: A): Future[A] =
      Future.successful(a)

    override def delay(duration: FiniteDuration) =
      Future.delayed(duration)(())

    override def attempt[A](_try: =>Future[A])(_catch: PartialFunction[Throwable, Future[A]]): Future[A] =
      _try.recoverWith(_catch)
    override def attemptFinally[A,U](_try: => Future[A])(_catch: PartialFunction[Throwable, Future[A]])(_finally: => Future[U]): Future[A] =
      _try.recoverWith(_catch).flatMap(a => _finally.map(_ => a))
    override def failure(t: Throwable): Future[Nothing] =
      Future.failed(t)
    override def success[A](a: A): Future[A] =
      Future.successful(a)
  }
}
