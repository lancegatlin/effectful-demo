package effectful.examples.pure.user.impl

import org.jasypt.digest.PooledStringDigester

import scala.concurrent.duration.FiniteDuration
import effectful.aspects._
import effectful.cats.{Capture, Monad}
import effectful.examples.pure.user.Passwords

class PasswordsImpl[E[_]](
  passwordMismatchDelay: FiniteDuration
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
      // Delay if password didn't match
      E.delay(passwordMismatchDelay).map(_ => false)
    }
  }

  override def mkDigest(plainTextPassword: String): E[String] = E.capture {
    digester.digest(plainTextPassword)
  }
}
