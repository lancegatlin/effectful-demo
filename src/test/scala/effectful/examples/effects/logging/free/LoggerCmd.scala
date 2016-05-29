package effectful.examples.effects.logging.free

sealed trait LoggerCmd[A]
object LoggerCmd {
  case class Trace(loggerName: String, message: String, cause: Option[Throwable]) extends LoggerCmd[Unit]
  case class Debug(loggerName: String, message: String, cause: Option[Throwable]) extends LoggerCmd[Unit]
  case class Info(loggerName: String, message: String, cause: Option[Throwable]) extends LoggerCmd[Unit]
  case class Warn(loggerName: String, message: String, cause: Option[Throwable]) extends LoggerCmd[Unit]
  case class Error(loggerName: String, message: String, cause: Option[Throwable]) extends LoggerCmd[Unit]
}