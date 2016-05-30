package effectful.examples.effects

import effectful._

package object logging {
  implicit object LiftService_Logger extends LiftService[Logger] {
    override def apply[E[_], F[_]](
      s: Logger[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): Logger[F] =
      new Logger[F] {
        override def trace(message: =>String): F[Unit] =
          liftCapture(s.trace(message))
        override def warn(message: =>String): F[Unit] =
          liftCapture(s.warn(message))
        override def warn(message: =>String, cause: Throwable): F[Unit] =
          liftCapture(s.warn(message,cause))
        override def error(message: =>String): F[Unit] =
          liftCapture(s.error(message))
        override def error(message: =>String, cause: Throwable): F[Unit] =
          liftCapture(s.error(message,cause))
        override def debug(message: =>String): F[Unit] =
          liftCapture(s.debug(message))
        override def debug(message: =>String, cause: Throwable): F[Unit] =
          liftCapture(s.debug(message))
        override def trace(message: =>String, cause: Throwable): F[Unit] =
          liftCapture(s.trace(message,cause))
        override def info(message: =>String): F[Unit] =
          liftCapture(s.info(message))
        override def info(message: => String, cause: Throwable): F[Unit] =
          liftCapture(s.info(message,cause))
      }
  }
}
