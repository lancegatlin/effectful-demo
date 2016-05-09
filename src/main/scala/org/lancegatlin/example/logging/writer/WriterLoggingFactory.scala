package org.lancegatlin.example.logging.writer

import org.lancegatlin.example.logging.{Logger, LoggerFactory}

object WriterLoggingFactory extends LoggerFactory[LogWriter] {
  override def mkLogger(name: String): Logger[LogWriter] =
    new WriterLogger(name)
}
