package effectful.examples.pure

import effectful._
import effectful.examples.pure.uuid.UUIDs.UUID

import scala.concurrent.duration.Duration
import scalaz.\/

package object user {
  implicit object LiftService_PasswordService extends LiftService[Passwords] {
    override def apply[E[_], F[_]](
      s: Passwords[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): Passwords[F] =
      new Passwords[F] {
        override def mkDigest(plainText: String) =
          liftCapture(s.mkDigest(plainText))
        override def compareDigest(d1: String, d2: String) =
          liftCapture(s.compareDigest(d1,d2))
      }
  }

  implicit object LiftService_TokenService extends LiftService[Tokens] {

    override def apply[E[_], F[_]](
      s: Tokens[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): Tokens[F] = {
      import Tokens._
      new Tokens[F] {
        override def issue(userId: UUID, deviceId: Option[UUID], expireAfter: Option[Duration]): F[(Token,TokenInfo)] =
          liftCapture(s.issue(userId,deviceId,expireAfter))
        override def validate(token: Token): F[Option[TokenInfo]] =
          liftCapture(s.validate(token))
        override def forceAllExpire(userId: UUID, exceptTokens: Token*): F[Boolean] =
          liftCapture(s.forceAllExpire(userId,exceptTokens:_*))
        override def find(token: Token): F[Option[TokenInfo]] =
          liftCapture(s.find(token))
        override def forceExpire(token: Token): F[Unit] =
          liftCapture(s.forceExpire(token))
      }
    }
  }

  implicit object LiftService_UserLoginService extends LiftService[UserLogins] {
    override def apply[E[_], F[_]](
      s: UserLogins[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): UserLogins[F] = {
      import UserLogins._
      new UserLogins[F] {
        override def login(username: String, password: String): F[\/[LoginFailure, Token]] =
          liftCapture(s.login(username,password))
      }
    }
  }
}
