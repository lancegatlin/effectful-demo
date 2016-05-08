package org.lancegatlin.effectful.logging.slf4j

import org.lancegatlin.effectful.logging.{Logger, LoggerFactory}

import scalaz.Id.Id

object Slf4jLoggerFactory extends LoggerFactory[Id] {
  override def mkLogger(name: String): Logger[Id] =
    new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name))
}
