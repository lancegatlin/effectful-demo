package effectful.examples.effects.logging.free

import cats._
import effectful.augments._
import effectful.examples.effects.logging.Logger
import effectful.free.Interpreter

class LoggerCmdInterpreter[E[_]](
  nameToLogger: String => Logger[E]
)(implicit
  val C:Capture[E],
  val M:Monad[E],
  val D:Delay[E],
  val P:Par[E],
  val X:Exceptions[E]
) extends Interpreter[LoggerCmd,E] {
  override def apply[A](cmd: LoggerCmd[A]): E[A] = {
    import LoggerCmd._
    // todo: logging commands should have logger as parm
    cmd match {
      case Trace(loggerName, message, Some(cause)) =>
        nameToLogger(loggerName).trace(message,cause)
      case Trace(loggerName, message, None) =>
        nameToLogger(loggerName).trace(message)
      case Debug(loggerName, message, Some(cause)) =>
        nameToLogger(loggerName).debug(message,cause)
      case Debug(loggerName, message, None) =>
        nameToLogger(loggerName).debug(message)
      case Info(loggerName, message, Some(cause)) =>
        nameToLogger(loggerName).info(message,cause)
      case Info(loggerName, message, cause) =>
        nameToLogger(loggerName).info(message)
      case Warn(loggerName, message, Some(cause)) =>
        nameToLogger(loggerName).warn(message,cause)
      case Warn(loggerName, message, None) =>
        nameToLogger(loggerName).warn(message)
      case Error(loggerName, message, Some(cause)) =>
        nameToLogger(loggerName).error(message,cause)
      case Error(loggerName, message, None) =>
        nameToLogger(loggerName).error(message)
    }
  } 
    
}
