package effectful.examples.pure.user.impl

import effectful._
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.user._

import scalaz.{-\/, \/, \/-}

class UserLoginServiceImpl[E[_]](
  logger: Logger[E],
  users: UserService[E],
  passwords: PasswordService[E],
  tokens: TokenService[E]
)(implicit
  E:Exec[E]
) extends UserLoginService[E] {
  import UserLoginService._
  import logger._

  override def login(username: String, password: String): E[LoginFailure \/ Token] =
    for {
      maybeUser <- users.findByUsername(username)
      result <- maybeUser match {
        case Some(user:UserService.User) =>
          for {
            digest <- passwords.mkDigest(password)
            passwordOk <- passwords.compareDigest(user.passwordDigest,digest)
            result <- {
              if(passwordOk) {
                for {
                  tuple <- tokens.issue(
                    userId = user.id,
                    deviceId = None,
                    expireAfter = None
                  )
                  (token,_) = tuple
                  _ <- info(s"User ${user.id} logged in")
                } yield \/-(token)
              } else {
                for {
                  _ <- warn(s"User ${user.id} password mismatch")
                } yield -\/(LoginFailure.PasswordMismatch)
              }
            }:E[LoginFailure \/ Token]
          } yield result
        case None =>
          E(-\/(LoginFailure.UserDoesNotExist))
      }
    } yield result
}
