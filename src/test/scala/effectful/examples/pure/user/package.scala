package effectful.examples.pure

import effectful._
import effectful.cats.Capture
import effectful.examples.pure.UUIDService.UUID

import scala.concurrent.duration.Duration
import scalaz.\/

package object user {
  implicit object LiftService_PasswordService$ extends LiftService[PasswordService] {
    override def apply[E[_], F[_]](
      s: PasswordService[E]
    )(implicit
      E: Capture[E],
      F: Capture[F],
      liftCapture: LiftCapture[E, F]
    ): PasswordService[F] =
      new PasswordService[F] {
        override def mkDigest(plainText: String) =
          liftCapture(s.mkDigest(plainText))
        override def compareDigest(d1: String, d2: String) =
          liftCapture(s.compareDigest(d1,d2))
      }
  }

  implicit object LiftService_TokenService$ extends LiftService[TokenService] {

    override def apply[E[_], F[_]](
      s: TokenService[E]
    )(implicit
      E: Capture[E],
      F: Capture[F],
      liftCapture: LiftCapture[E, F]
    ): TokenService[F] = {
      import TokenService._
      new TokenService[F] {
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

  implicit object LiftService_UserLoginService$ extends LiftService[UserLoginService] {
    override def apply[E[_], F[_]](
      s: UserLoginService[E]
    )(implicit
      E: Capture[E],
      F: Capture[F],
      liftCapture: LiftCapture[E, F]
    ): UserLoginService[F] = {
      import UserLoginService._
      new UserLoginService[F] {
        override def login(username: String, password: String): F[\/[LoginFailure, Token]] =
          liftCapture(s.login(username,password))
      }
    }
  }
}
