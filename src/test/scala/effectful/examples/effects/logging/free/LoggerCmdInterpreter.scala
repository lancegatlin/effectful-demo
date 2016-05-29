package effectful.examples.effects.logging.free

import effectful.Exec
import effectful.examples.effects.logging.Logger
import effectful.free.Interpreter

class LoggerCmdInterpreter[E[_]](
  // todo: this should be LoggerFactory
  nameToLogger: Map[String,Logger[E]]                                
)(implicit
  val E:Exec[E]
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
