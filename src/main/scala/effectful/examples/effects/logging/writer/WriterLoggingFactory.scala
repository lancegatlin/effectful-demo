package effectful.examples.effects.logging.writer

import effectful.examples.effects.logging.{Logger, LoggerFactory}

object WriterLoggingFactory extends LoggerFactory[LogWriter] {
  override def mkLogger(name: String): Logger[LogWriter] =
    new WriterLogger(name)
}
