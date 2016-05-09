package org.lancegatlin.example.user.impl

import java.time.Instant

import scala.language.higherKinds
import java.time.temporal.ChronoUnit.MILLIS

import scala.concurrent.duration.Duration
import org.lancegatlin.effectful._
import org.lancegatlin.example.logging.Logger
import org.lancegatlin.example.{UUID, UUIDService}
import org.lancegatlin.example.db.EntityService
import org.lancegatlin.example.db.query._
import org.lancegatlin.example.user.TokenService

object TokenServiceImpl {
  import TokenService._
  object TokenInfoFields {
    val userId = Query.Field("userId",(_:TokenInfo).userId)
    val deviceId = Query.Field("deviceId",(_:TokenInfo).deviceId)
    val lastValidated = Query.Field("lastValidated",(_:TokenInfo).lastValidated)
    val expiresOn = Query.Field("expiresOn",(_:TokenInfo).expiresOn)
    val allFields = Seq(userId,deviceId,lastValidated,expiresOn)
  }
}

class TokenServiceImpl[E[_]](
  logger: Logger[E],
  uuids: UUIDService[E],
  tokens: EntityService[String,TokenService.TokenInfo,E],
  tokenDefaultDuration: Duration
)(implicit
  E:EffectSystem[E]
) extends TokenService[E] {
  import TokenService._
  import TokenServiceImpl._
  import logger._

  override def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ): E[TokenInfo] =
    for {
      uuid <- uuids.gen()
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
      _ <- {
        if(result == false) {
          E(throw new RuntimeException("Failed to create token"))
        } else {
          E(())
        }
      }:E[Unit] // Note: fix intellij erroneous error
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
      result <- {
        optTokenInfo match {
          case Some((tokenInfo,_)) =>
            tokens.update(token, tokenInfo.copy(
              expiresOn = Instant.now()
            ))
          case None => E(throw new RuntimeException("Token does not exist"))
        }
      }:E[Boolean] // Note: fix intellij erroneous error
      _ <- {
        if(result == false) {
          E(throw new RuntimeException("Failed to update token"))
        } else {
          E(())
        }
      }
    } yield ()

  override def forceAllExpire(userId: UUID, exceptTokens: String*): E[Boolean] =
    for {
      allTokenInfo <- tokens.find {
        TokenInfoFields.userId === userId
      }
      _ <- allTokenInfo.map { case (token,_,_) =>
        forceExpire(token)
      }.sequence
    } yield true

}
