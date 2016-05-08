package org.lancegatlin.example.impl

import java.time.Instant

import scala.language.higherKinds
import org.lancegatlin.effectful._
import org.lancegatlin.effectful.logging.Logger
import org.lancegatlin.example.{EntityService, TokenService, UUID, UUIDService}
import org.lancegatlin.example.TokenService.TokenInfo

import scala.concurrent.duration.Duration
import java.time.temporal.ChronoUnit.MILLIS

class TokenServiceImpl[E[_]](
  logger: Logger[E],
  uuids: UUIDService[E],
  tokens: EntityService[String,TokenInfo,E],
  tokenDefaultDuration: Duration
)(implicit
  E:EffectSystem[E]
) extends TokenService[E] {
  import logger._

  override def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ): E[TokenInfo] =
    for {
      uuid <- uuids()
      tokenInfo = TokenInfo(
        userId = userId,
        deviceId = deviceId,
        lastValidated = Instant.now(),
        expiresOn = Instant.now().plus(
          expireAfter.getOrElse(tokenDefaultDuration).toMillis,
          MILLIS
        )
      )
      result <- tokens.insert(uuid.toString, tokenInfo)
      _ <- if(result == false) {
        E(throw new RuntimeException("Failed to create token"))
      } else {
        E(())
      }
      _ <- info(s"Issued token $uuid to user $userId")
    } yield tokenInfo


  override def validate(token: String): E[Option[TokenInfo]] =
    for {
      optTokenInfo <- tokens.findById(token)
      result <- optTokenInfo match {
        case Some((tokenInfo,_)) if tokenInfo.expiresOn.compareTo(Instant.now()) > 0 =>
          tokens.update(token, tokenInfo.copy(
            lastValidated = Instant.now()
          ))
        case None => E(true)
      }
      _ <- if(result == false) {
        E(throw new RuntimeException("Failed to update token"))
      } else {
        E(())
      }
      _ <- info(s"Validated token $token for user ${optTokenInfo.get._1.userId}")
    } yield optTokenInfo.map(_._1)

  override def find(token: String): E[Option[TokenInfo]] =
    tokens.findById(token).map(_.map(_._1))

  override def forceExpire(token: String): E[Unit] =
    for {
      optTokenInfo <- tokens.findById(token)
      result <- optTokenInfo match {
        case Some((tokenInfo,_)) =>
          tokens.update(token, tokenInfo.copy(
            expiresOn = Instant.now()
          ))
        case None => E(throw new RuntimeException("Token does not exist"))
      }
      _ <- if(result == false) {
        E(throw new RuntimeException("Failed to update token"))
      } else {
        E(())
      }
    } yield ()

  // Note: need general way to query EntityService to implement this
  override def forceAllExpire(userId: UUID, exceptTokens: String*): E[Boolean] = ???

}
