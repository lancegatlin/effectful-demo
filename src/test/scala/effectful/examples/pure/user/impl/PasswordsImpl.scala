package effectful.examples.pure.user.impl

import org.jasypt.digest.PooledStringDigester

import scala.concurrent.duration.FiniteDuration
import effectful.augments._
import effectful.cats.{Capture, Monad}
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.user.Passwords

class PasswordsImpl[E[_]](
  passwordMismatchDelay: FiniteDuration,
  logger: Logger[E]
)(implicit
  E:Monad[E],
  C:Capture[E],
  D:Delay[E]
) extends Passwords[E] {
  import Monad.ops._

  val digester = new PooledStringDigester()
  digester.setPoolSize(Runtime.getRuntime.availableProcessors())
  digester.initialize()

  override def compareDigest(plainTextPassword: String, passwordDigest: String): E[Boolean] = {
    if(digester.matches(plainTextPassword, passwordDigest)) {
      E.pure(true)
    } else {
      for {
        _ <- logger.warn(s"Password mismatch delaying $passwordMismatchDelay")
        _ <- E.delay(passwordMismatchDelay).map(_ => false)
      } yield false
    }
  }

  override def mkDigest(plainTextPassword: String): E[String] = E.capture {
    digester.digest(plainTextPassword)
  }
}
