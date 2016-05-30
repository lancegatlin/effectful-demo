package effectful.examples.adapter.slf4j

import effectful.examples.effects.logging.Logger

import scalaz.Id.Id

class Slf4jLogger(underlying: org.slf4j.Logger) extends Logger[Id] {
  override def trace(message: => String) : Id[Unit] =
    if(underlying.isTraceEnabled) {
      underlying.trace(message)
    }

  override def trace(message: => String, cause: Throwable): Id[Unit] =
    if(underlying.isTraceEnabled) {
      underlying.trace(message,cause)
    }

  override def debug(message: => String): Id[Unit] =
    if(underlying.isDebugEnabled) {
      underlying.debug(message)
    }

  override def debug(message: => String, cause: Throwable): Id[Unit] =
    if(underlying.isDebugEnabled) {
      underlying.debug(message,cause)
    }

  override def warn(message: => String): Id[Unit] =
    if(underlying.isWarnEnabled) {
      underlying.warn(message)
    }

  override def warn(message: => String, cause: Throwable): Id[Unit] =
    if(underlying.isWarnEnabled) {
      underlying.warn(message,cause)
    }

  override def error(message: => String): Id[Unit] =
    if(underlying.isErrorEnabled) {
      underlying.error(message)
    }

  override def error(message: => String, cause: Throwable): Id[Unit] =
    if(underlying.isErrorEnabled) {
      underlying.error(message,cause)
    }

  override def info(message: => String): Id[Unit] =
    if(underlying.isInfoEnabled) {
      underlying.info(message)
    }

  override def info(message: => String, cause: Throwable): Id[Unit] =
    if(underlying.isInfoEnabled) {
      underlying.info(message,cause)
    }

}
