package effectful.examples.effects.logging.free

import scala.language.higherKinds
import effectful.examples.effects.logging.{Logger, LoggerFactory}

object FreeLoggerFactory extends LoggerFactory[FreeLoggingCmd] {
  override def mkLogger(name: String): Logger[FreeLoggingCmd] =
    new FreeLogger(name)
}
