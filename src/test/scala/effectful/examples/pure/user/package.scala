package effectful.examples.pure

import effectful._
import effectful.examples.pure.UUIDService.UUID

import scala.concurrent.duration.Duration
import scalaz.\/

package object user {
  implicit object LiftS_PasswordService extends LiftS[PasswordService] {
    override def apply[E[_], F[_]](
      s: PasswordService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): PasswordService[F] =
      new PasswordService[F] {
        override def mkDigest(plainText: String) =
          liftE(s.mkDigest(plainText))
        override def compareDigest(d1: String, d2: String) =
          liftE(s.compareDigest(d1,d2))
      }
  }

  implicit object LiftS_TokenService extends LiftS[TokenService] {

    override def apply[E[_], F[_]](
      s: TokenService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): TokenService[F] = {
      import TokenService._
      new TokenService[F] {
        override def issue(userId: UUID, deviceId: Option[UUID], expireAfter: Option[Duration]): F[(Token,TokenInfo)] =
          liftE(s.issue(userId,deviceId,expireAfter))
        override def validate(token: Token): F[Option[TokenInfo]] =
          liftE(s.validate(token))
        override def forceAllExpire(userId: UUID, exceptTokens: Token*): F[Boolean] =
          liftE(s.forceAllExpire(userId,exceptTokens:_*))
        override def find(token: Token): F[Option[TokenInfo]] =
          liftE(s.find(token))
        override def forceExpire(token: Token): F[Unit] =
          liftE(s.forceExpire(token))
      }
    }
  }

  implicit object LiftS_UserLoginService extends LiftS[UserLoginService] {
    override def apply[E[_], F[_]](
      s: UserLoginService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): UserLoginService[F] = {
      import UserLoginService._
      new UserLoginService[F] {
        override def login(username: String, password: String): F[\/[LoginFailure, Token]] =
          liftE(s.login(username,password))
      }
    }
  }
}