package effectful.examples.adapter

import effectful.examples.effects.logging.writer.LogWriter
import effectful.{EffectSystem, LiftE}

import scala.concurrent.{ExecutionContext, Future}

package object akka {
  implicit def effectSystem_Future(implicit ec:ExecutionContext) = new EffectSystem[Future] {
    override def map[A, B](m: Future[A])(f: (A) => B): Future[B] =
      m.map(f)

    override def flatMap[A, B](m: Future[A])(f: (A) => Future[B]): Future[B] =
      m.flatMap(f)

    override def Try[A](_try: =>Future[A])(_catch: PartialFunction[Throwable, Future[A]]): Future[A] =
      _try.recoverWith(_catch)

    override def TryFinally[A,U](_try: => Future[A])(_catch: PartialFunction[Throwable, Future[A]])(_finally: => Future[U]): Future[A] =
      _try.recoverWith(_catch).flatMap(a => _finally.map(_ => a))

    override def widen[A, AA >: A](ea: Future[A]): Future[AA] =
      ea

    override def apply[A](a: => A): Future[A] = Future(a)
  }

  type FutureLogWriter[A] = Future[LogWriter[A]]
  
  implicit object liftE_Writer_Future extends LiftE[LogWriter,FutureLogWriter] {
    override def apply[A](
      ea: => LogWriter[A]
    )(implicit
      E: EffectSystem[LogWriter],
      F: EffectSystem[FutureLogWriter]
    ): FutureLogWriter[A] =
      Future.successful(ea)
  }

  implicit def liftE_Future_FutureLogWriter(implicit ec:ExecutionContext) = new LiftE[Future,FutureLogWriter] {
    override def apply[A](
      ea: => Future[A]
    )(implicit
      E: EffectSystem[Future],
      F: EffectSystem[FutureLogWriter]
    ): FutureLogWriter[A] =
      ea.map(a => LogWriter(a))
  }

}
