package effectful.examples.effects.logging.writer

import effectful.examples.effects.logging.Logger
import scalaz.Writer

class WriterLogger(name: String) extends Logger[LogWriter] {
  override def trace(message: =>String): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Trace,message,None) :: Nil,())

  override def trace(message: =>String, cause: Throwable): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Trace,message,Some(cause)) :: Nil,())

  override def debug(message: =>String): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Debug,message,None) :: Nil,())

  override def debug(message: =>String, cause: Throwable): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Debug,message,Some(cause)) :: Nil,())

  override def info(message: =>String): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Info,message,None) :: Nil,())

  override def info(message: => String, cause: Throwable): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Info,message,None) :: Nil,())

  override def warn(message: =>String): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Warn,message,None) :: Nil,())

  override def warn(message: =>String, cause: Throwable): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Warn,message,Some(cause)) :: Nil,())

  override def error(message: =>String): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Error,message,None) :: Nil,())

  override def error(message: =>String, cause: Throwable): LogWriter[Unit] =
    Writer(LogEntry(name,LogLevel.Error,message,Some(cause)) :: Nil,())


}
