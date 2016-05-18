package effectful.examples.pure.user.impl

import scala.language.higherKinds
import org.jasypt.digest.PooledStringDigester

import scala.concurrent.duration.FiniteDuration
import effectful._
import effectful.examples.effects.delay.DelayService
import effectful.examples.pure.user.PasswordService

class PasswordServiceImpl[E[_]](
  delayService: DelayService[E],
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
      // Delay if password didn't match
      delayService.delay(passwordMismatchDelay).map(_ => false)
    }
  }

  override def mkDigest(plainText: String): E[String] = E {
    digester.digest(plainText)
  }
}
