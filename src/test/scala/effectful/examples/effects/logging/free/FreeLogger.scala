package effectful.examples.effects.logging.free

import effectful.Free
import effectful.examples.effects.logging.Logger

class FreeLogger(logger: String) extends Logger[FreeLoggingCmd] {
  override def trace(message: =>String) =
    Free.Command(LoggingCmd.Trace(message,None))

  override def trace(message: =>String, cause: Throwable): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Trace(message,Some(cause)))

  override def debug(message: =>String): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Debug(message,None))

  override def debug(message: =>String, cause: Throwable): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Debug(message,Some(cause)))

  override def info(message: =>String): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Info(message,None))

  override def info(message: => String, cause: Throwable): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Info(message,Some(cause)))

  override def warn(message: =>String): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Warn(message,None))

  override def warn(message: =>String, cause: Throwable): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Warn(message,Some(cause)))

  override def error(message: =>String): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Error(message,None))

  override def error(message: =>String, cause: Throwable): FreeLoggingCmd[Unit] =
    Free.Command(LoggingCmd.Error(message,Some(cause)))
}
