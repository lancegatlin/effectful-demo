package org.lancegatlin.effectful.logging.free

import org.lancegatlin.effectful.Free
import org.lancegatlin.effectful.logging.Logger

class FreeLogger(logger: String) extends Logger[FreeLogging] {
  override def trace(message: =>String) =
    Free.Effect(LoggingCmd.Trace(logger,message,None))

  override def trace(message: =>String, cause: Throwable): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Trace(logger,message,Some(cause)))

  override def debug(message: =>String): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Debug(logger,message,None))

  override def debug(message: =>String, cause: Throwable): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Debug(logger,message,Some(cause)))

  override def info(message: =>String): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Info(logger,message,None))

  override def info(message: => String, cause: Throwable): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Info(logger,message,Some(cause)))

  override def warn(message: =>String): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Warn(logger,message,None))

  override def warn(message: =>String, cause: Throwable): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Warn(logger,message,Some(cause)))

  override def error(message: =>String): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Error(logger,message,None))

  override def error(message: =>String, cause: Throwable): FreeLogging[Unit] =
    Free.Effect(LoggingCmd.Error(logger,message,Some(cause)))
}
