package effectful.examples.pure.user.impl

import org.jasypt.digest.PooledStringDigester

import scala.concurrent.duration.FiniteDuration
import effectful.augments._
import cats.Monad
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
  import logger._
  val eMonadMonadless = io.monadless.cats.MonadlessMonad[E]()
  import eMonadMonadless._

  val digester = new PooledStringDigester()
  digester.setPoolSize(Runtime.getRuntime.availableProcessors())
  digester.initialize()

  override def compareDigest(plainTextPassword: String, passwordDigest: String): E[Boolean] =
    lift {
      if(digester.matches(plainTextPassword, passwordDigest)) {
        true
      } else {
        unlift(warn(s"Password mismatch delaying $passwordMismatchDelay"))
        unlift(E.delay(passwordMismatchDelay).map(_ => false))
        false
      }
    }

  override def mkDigest(plainTextPassword: String): E[String] =
    E.capture {
      digester.digest(plainTextPassword)
    }
}
