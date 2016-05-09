package org.lancegatlin.example.user

import java.time.Instant
import scala.language.higherKinds
import org.lancegatlin.example.UUID
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