package effectful.examples.effects.logging.free

sealed trait LoggingCmd[A]
object LoggingCmd {
  case class Trace(message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Debug(message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Info(message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Warn(message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Error(message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
}