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
}