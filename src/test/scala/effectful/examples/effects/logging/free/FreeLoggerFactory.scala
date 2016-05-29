package effectful.examples.effects.logging.free

import effectful.examples.effects.logging.{Logger, LoggerFactory}

object FreeLoggerFactory extends LoggerFactory[FreeLoggerCmd] {
  override def mkLogger(name: String): Logger[FreeLoggerCmd] =
    new FreeLogger(name)
}
