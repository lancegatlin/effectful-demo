package org.lancegatlin.effectful.logging.free

import scala.language.higherKinds
import org.lancegatlin.effectful.logging.{Logger, LoggerFactory}

object FreeLoggerFactory extends LoggerFactory[FreeLogging] {
  override def mkLogger(name: String): Logger[FreeLogging] =
    new FreeLogger(name)
}
