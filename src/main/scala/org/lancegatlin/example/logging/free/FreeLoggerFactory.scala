package org.lancegatlin.example.logging.free

import scala.language.higherKinds
import org.lancegatlin.example.logging.{Logger, LoggerFactory}

object FreeLoggerFactory extends LoggerFactory[FreeLogging] {
  override def mkLogger(name: String): Logger[FreeLogging] =
    new FreeLogger(name)
}
