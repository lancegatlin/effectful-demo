package org.lancegatlin.effectful.logging.writer

import org.lancegatlin.effectful.logging.{Logger, LoggerFactory}

object WriterLoggingFactory extends LoggerFactory[LogWriter] {
  override def mkLogger(name: String): Logger[LogWriter] =
    new WriterLogger(name)
}
