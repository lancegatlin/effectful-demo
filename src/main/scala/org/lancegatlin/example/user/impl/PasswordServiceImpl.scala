package org.lancegatlin.example.user.impl

import scala.language.higherKinds
import org.jasypt.digest.PooledStringDigester
import scala.concurrent.duration.FiniteDuration
import org.lancegatlin.effectful._
import org.lancegatlin.example.user.PasswordService

class PasswordServiceImpl[E[_]](
  passwordMismatchDelay: FiniteDuration
)(implicit
  E:EffectSystem[E]
) extends PasswordService[E] {
  val digester = new PooledStringDigester()
  digester.setPoolSize(Runtime.getRuntime.availableProcessors())
  digester.initialize()

  override def compareDigest(d1: String, d2: String): E[Boolean] = {
    if(digester.matches(d1, d2)) {
      E(true)
    } else {
      E.delay(passwordMismatchDelay).map(_ => false)
    }
  }

  override def mkDigest(plainText: String): E[String] = E {
    digester.digest(plainText)
  }
}
