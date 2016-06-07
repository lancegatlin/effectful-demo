package effectful.examples.effects.logging.free

import effectful.examples.effects.logging.Logger
import effectful.free.Free

class FreeLogger(loggerName: String) extends Logger[FreeLoggerCmd] {
  override def trace(message: =>String) =
    Free.Command(LoggerCmd.Trace(loggerName,message,None))

  override def trace(message: =>String, cause: Throwable): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Trace(loggerName,message,Some(cause)))

  override def debug(message: =>String): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Debug(loggerName,message,None))

  override def debug(message: =>String, cause: Throwable): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Debug(loggerName,message,Some(cause)))

  override def info(message: =>String): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Info(loggerName,message,None))

  override def info(message: => String, cause: Throwable): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Info(loggerName,message,Some(cause)))

  override def warn(message: =>String): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Warn(loggerName,message,None))

  override def warn(message: =>String, cause: Throwable): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Warn(loggerName,message,Some(cause)))

  override def error(message: =>String): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Error(loggerName,message,None))

  override def error(message: =>String, cause: Throwable): FreeLoggerCmd[Unit] =
    Free.Command(LoggerCmd.Error(loggerName,message,Some(cause)))
}

object FreeLogger {
  def apply(loggerName: String) : FreeLogger =
    new FreeLogger(loggerName)
}