package org.lancegatlin.example.impl

import org.jasypt.digest.PooledStringDigester
import org.lancegatlin.example.PasswordService
import org.lancegatlin.effectful._

class PasswordServiceImpl extends PasswordService[Id] {
  val digester = new PooledStringDigester()
  digester.setPoolSize(Runtime.getRuntime.availableProcessors())
  digester.initialize()

  override def compareDigest(d1: String, d2: String): Id[Boolean] = {
    digester.matches(d1, d2)
  }

  override def mkDigest(plainText: String): Id[String] = {
    digester.digest(plainText)
  }
}
