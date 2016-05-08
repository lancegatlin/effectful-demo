package org.lancegatlin.effectful.logging.writer

case class LogEntry(
  logger: String, 
  level: LogLevel,
  message: String, 
  cause: Option[Throwable]
)

sealed trait LogLevel
object LogLevel {
  case object Trace extends LogLevel
  case object Debug extends LogLevel
  case object Info extends LogLevel
  case object Warn extends LogLevel
  case object Error extends LogLevel
}