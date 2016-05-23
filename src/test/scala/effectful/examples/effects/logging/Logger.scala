package effectful.examples.effects.logging

import effectful._

trait Logger[E[_]] {
  def trace(message: => String) : E[Unit]
  def trace(message: => String, cause: Throwable) : E[Unit]
  def debug(message: => String) : E[Unit]
  def debug(message: => String, cause: Throwable) : E[Unit]
  def info(message: => String) : E[Unit]
  def info(message: => String, cause: Throwable) : E[Unit]
  def warn(message: => String) : E[Unit]
  def warn(message: => String, cause: Throwable) : E[Unit]
  def error(message: => String) : E[Unit]
  def error(message: => String, cause: Throwable) : E[Unit]
}

object Logger {
  implicit object LiftS_Logger extends LiftS[Logger] {
    override def apply[E[_], F[_]](
      s: Logger[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): Logger[F] =
      new Logger[F] {
        override def trace(message: =>String): F[Unit] =
          liftE(s.trace(message))
        override def warn(message: =>String): F[Unit] =
          liftE(s.warn(message))
        override def warn(message: =>String, cause: Throwable): F[Unit] =
          liftE(s.warn(message,cause))
        override def error(message: =>String): F[Unit] =
          liftE(s.error(message))
        override def error(message: =>String, cause: Throwable): F[Unit] =
          liftE(s.error(message,cause))
        override def debug(message: =>String): F[Unit] =
          liftE(s.debug(message))
        override def debug(message: =>String, cause: Throwable): F[Unit] =
          liftE(s.debug(message))
        override def trace(message: =>String, cause: Throwable): F[Unit] =
          liftE(s.trace(message,cause))
        override def info(message: =>String): F[Unit] =
          liftE(s.info(message))
        override def info(message: => String, cause: Throwable): F[Unit] =
          liftE(s.info(message,cause))
      }
  }  
}