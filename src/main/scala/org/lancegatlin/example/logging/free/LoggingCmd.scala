package org.lancegatlin.example.logging.free

sealed trait LoggingCmd[A]
object LoggingCmd {
  case class Trace(logger: String, message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Debug(logger: String, message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Info(logger: String, message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Warn(logger: String, message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
  case class Error(logger: String, message: String, cause: Option[Throwable]) extends LoggingCmd[Unit]
}