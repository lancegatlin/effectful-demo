package effectful.examples.pure.user

import java.time.Instant

import effectful.examples.pure.UUIDService.UUID

import scala.language.higherKinds
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
}