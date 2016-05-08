package org.lancegatlin.example

import scala.language.higherKinds
import java.time.Instant

import org.lancegatlin.effectful.{EffectSystem, LiftE, LiftS}

import scala.concurrent.duration.Duration

trait TokenService[E[_]] {
  import TokenService._

  def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ) : E[TokenInfo]

  def validate(
    token: String
  ) : E[Option[TokenInfo]]

  def find(
    token: String
  ) : E[Option[TokenInfo]]

  def forceExpire(
    token: String
  ) : E[Unit]

  def forceAllExpire(
    userId: UUID,
    exceptTokens: String*
  ) : E[Boolean]
}

object TokenService {
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
        override def issue(userId: UUID, deviceId: Option[UUID], expireAfter: Option[Duration]): F[TokenInfo] =
          liftE(s.issue(userId,deviceId,expireAfter))
        override def validate(token: String): F[Option[TokenInfo]] =
          liftE(s.validate(token))
        override def forceAllExpire(userId: UUID, exceptTokens: String*): F[Boolean] =
          liftE(s.forceAllExpire(userId,exceptTokens:_*))
        override def find(token: String): F[Option[TokenInfo]] =
          liftE(s.find(token))
        override def forceExpire(token: String): F[Unit] =
          liftE(s.forceExpire(token))
      }
  }
}