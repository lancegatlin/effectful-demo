package effectful.examples.effects.logging.free

import scala.language.higherKinds
import effectful.examples.effects.logging.{Logger, LoggerFactory}

object FreeLoggerFactory extends LoggerFactory[FreeLogging] {
  override def mkLogger(name: String): Logger[FreeLogging] =
    new FreeLogger(name)
}
