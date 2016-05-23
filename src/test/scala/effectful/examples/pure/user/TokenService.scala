package effectful.examples.pure.user

import java.time.Instant

import effectful.{EffectSystem, LiftE, LiftS}
import effectful.examples.pure.UUIDService.UUID

import scala.concurrent.duration.Duration

trait TokenService[E[_]] {
  import TokenService._

  def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ) : E[(Token,TokenInfo)]

  def validate(
    token: Token
  ) : E[Option[TokenInfo]]

  def find(
    token: Token
  ) : E[Option[TokenInfo]]

  def forceExpire(
    token: Token
  ) : E[Unit]

  def forceAllExpire(
    userId: UUID,
    exceptTokens: Token*
  ) : E[Boolean]
}

object TokenService {
  type Token = String
  case class TokenInfo(
    userId: UUID,
    deviceId: Option[UUID],
    lastValidated: Instant,
    expiresOn: Instant
  )

  implicit object LiftS_TokenService extends LiftS[TokenService] {

    override def apply[E[_], F[_]](
      s: TokenService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): TokenService[F] =
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