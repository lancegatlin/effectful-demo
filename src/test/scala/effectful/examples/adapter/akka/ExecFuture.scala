package effectful.examples.adapter.akka

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import effectful.augments._
import cats._
import effectful.impl.StdPar
import s_mach.concurrent._

object ExecFuture {
  def bindContext()(implicit
    ec: ExecutionContext,
    ses:ScheduledExecutionContext,
    _E: Monad[Future]
  ) :   Capture[Future] with
        Exceptions[Future] with
        Par[Future] with
        Delay[Future] =
    new
        Capture[Future] with
        Exceptions[Future] with
        StdPar[Future] with
        Delay[Future] {
    implicit val E = _E

    override def capture[A](a: => A) = Future(a)

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
