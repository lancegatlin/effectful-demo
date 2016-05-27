package effectful.examples.effects.logging.free

import effectful.Exec
import effectful.examples.effects.logging.Logger
import effectful.free.Interpreter

class LoggerCmdInterpreter[E[_]](
  // todo: this should be LoggerFactory
  logger: Logger[E]                                
)(implicit
  val E:Exec[E]
) extends Interpreter[LoggingCmd,E] {
  override def apply[A](cmd: LoggingCmd[A]): E[A] = {
    import LoggingCmd._
    // todo: logging commands should have logger as parm
    cmd match {
      case Trace(message, Some(cause)) =>
        logger.trace(message,cause)
      case Trace(message, None) =>
        logger.trace(message)
      case Debug(message, Some(cause)) =>
        logger.debug(message,cause)
      case Debug(message, None) =>
        logger.debug(message)
      case Info(message, Some(cause)) =>
        logger.info(message,cause)
      case Info(message, cause) =>
        logger.info(message)
      case Warn(message, Some(cause)) =>
        logger.warn(message,cause)
      case Warn(message, None) =>
        logger.warn(message)
      case Error(message, Some(cause)) =>
        logger.error(message,cause)
      case Error(message, None) =>
        logger.error(message)
    }
  } 
    
}
