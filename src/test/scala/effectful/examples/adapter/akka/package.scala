package effectful.examples.adapter

import effectful.cats.{Capture, Monad}
import effectful.examples.adapter.scalaz.writer.LogWriter
import effectful.impl.StdPar
import effectful.{Exec, LiftCapture}
import s_mach.concurrent._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

package object akka {
  implicit def exec_Future(implicit
    ec:ExecutionContext,
    sc:ScheduledExecutionContext
  ) = new Exec[Future] with StdPar[Future] {

    override implicit val E : Monad[Future] = this

    def capture[A](a: => A) = Future(a)

    override def map[A, B](m: Future[A])(f: (A) => B): Future[B] =
      m.map(f)

    override def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] =
      m.flatMap(f)

    override def widen[A, AA >: A](ea: Future[A]): Future[AA] =
      ea

    override def pure[A](a: A): Future[A] =
      Future.successful(a)

    override def attempt[A](_try: =>Future[A])(_catch: PartialFunction[Throwable, Future[A]]): Future[A] =
      _try.recoverWith(_catch)

    override def attemptFinally[A,U](_try: => Future[A])(_catch: PartialFunction[Throwable, Future[A]])(_finally: => Future[U]): Future[A] =
      _try.recoverWith(_catch).flatMap(a => _finally.map(_ => a))


    override def failure(t: Throwable): Future[Nothing] =
      Future.failed(t)

    override def success[A](a: A): Future[A] =
      Future.successful(a)

    override def delay(duration: FiniteDuration) = {
      Future.delayed(duration)(())
    }
  }

  type FutureLogWriter[A] = Future[LogWriter[A]]
  
  implicit object liftCapture_Writer_Future$ extends LiftCapture[LogWriter,FutureLogWriter] {
    override def apply[A](
      ea: => LogWriter[A]
    )(implicit
      E: Capture[LogWriter],
      F: Capture[FutureLogWriter]
    ): FutureLogWriter[A] =
      Future.successful(ea)
  }

  implicit def liftE_Future_FutureLogWriter(implicit ec:ExecutionContext) = new LiftCapture[Future,FutureLogWriter] {
    override def apply[A](
      ea: => Future[A]
    )(implicit
      E: Capture[Future],
      F: Capture[FutureLogWriter]
    ): FutureLogWriter[A] =
      ea.map(a => LogWriter(a))
  }

}
