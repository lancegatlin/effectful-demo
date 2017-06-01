package effectful.examples.pure.user.impl

import cats.Monad
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.user._

class UserLoginsImpl[E[_]](
  users: Users[E],
  passwords: Passwords[E],
  tokens: Tokens[E],
  logger: Logger[E]
)(implicit
  E:Monad[E]
) extends UserLogins[E] {
  import Monad.ops._
  import UserLogins._
  import logger._

  override def login(username: String, password: String): E[Either[LoginFailure,Token]] =
    for {
      maybeUser <- users.findByUsername(username)
      result <- maybeUser match {
        case Some(user:Users.User) =>
          for {
            passwordOk <- passwords.compareDigest(password,user.passwordDigest)
            result <- {
              if(passwordOk) {
                for {
                  tuple <- tokens.issue(
                    userId = user.id,
                    deviceId = None,
                    expireAfter = None
                  )
                  (token,_) = tuple
                  _ <- info(s"User ${user.id} logged in, issued token $token")
                } yield Right(token)
              } else {
                for {
                  _ <- warn(s"User ${user.id} password mismatch")
                } yield Left(LoginFailure.PasswordMismatch)
              }
            }:E[Either[LoginFailure,Token]]
          } yield result
        case None =>
          E.pure(Left(LoginFailure.UserDoesNotExist))
      }
    } yield result
}
