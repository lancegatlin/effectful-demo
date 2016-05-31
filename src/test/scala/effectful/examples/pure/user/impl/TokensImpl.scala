package effectful.examples.pure.user.impl

import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS

import scala.concurrent.duration.Duration
import effectful._
import effectful.cats.Monad
import effectful.examples.pure.dao.DocDao
import effectful.examples.pure.dao.query.Query
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.dao.query._
import effectful.examples.pure.user.Tokens
import effectful.examples.pure.uuid.UUIDs
import effectful.examples.pure.uuid.UUIDs.UUID

object TokensImpl {
  import Tokens._
  object TokenInfoFields {
    val userId = Query.Field("userId",(_:TokenInfo).userId)
    val deviceId = Query.Field("deviceId",(_:TokenInfo).deviceId)
    val lastValidated = Query.Field("lastValidated",(_:TokenInfo).lastValidated)
    val expiresOn = Query.Field("expiresOn",(_:TokenInfo).expiresOn)
    val allFields = Seq(userId,deviceId,lastValidated,expiresOn)
  }
}

class TokensImpl[E[_]](
  logger: Logger[E],
  uuids: UUIDs[E],
  tokens: DocDao[String,Tokens.TokenInfo,E],
  tokenDefaultDuration: Duration
)(implicit
  E:Monad[E]
) extends Tokens[E] {
  import Monad.ops._
  import Tokens._
  import TokensImpl._
  import logger._

  override def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ): E[(Token,TokenInfo)] =
    for {
      uuid <- uuids.gen()
      token = uuid.toString
      tokenInfo = TokenInfo(
        userId = userId,
        deviceId = deviceId,
        lastValidated = Instant.now(),
        expiresOn = Instant.now().plus(
          expireAfter.getOrElse(tokenDefaultDuration).toMillis,
          MILLIS
        )
      )
      result <- tokens.insert(token, tokenInfo)
      _ <- {
        if(result == false) {
          E(throw new RuntimeException("Failed to create token"))
        } else {
          E(())
        }
      }:E[Unit] // Note: fix intellij erroneous error
      _ <- info(s"Issued token $uuid to user $userId")
    } yield (token,tokenInfo)


  override def validate(token: String): E[Option[TokenInfo]] =
    for {
      optTokenInfo <- tokens.findById(token)
      result <- optTokenInfo match {
        case Some((_,tokenInfo,_)) if tokenInfo.expiresOn.compareTo(Instant.now()) > 0 =>
          for {
            result <- tokens.update(token, tokenInfo.copy(
              lastValidated = Instant.now()
            ))
            _ <- {
              if(result) {
                info(s"Validated token $token for user ${optTokenInfo.get._2.userId}")
              } else {
                E(throw new RuntimeException("Failed to update token"))
              }
            }:E[Unit]
          } yield Some(tokenInfo)
        case _ => E(None)
      }
    } yield result

  override def find(token: Token): E[Option[TokenInfo]] =
    tokens.findById(token).map(_.map(_._2))

  override def forceExpire(token: Token): E[Unit] =
    for {
      optTokenInfo <- tokens.findById(token)
      result <- {
        optTokenInfo match {
          case Some((_,tokenInfo,_)) =>
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

  override def forceAllExpire(userId: UUID, exceptTokens: Token*): E[Boolean] =
    for {
      allTokenInfo <- tokens.find {
        TokenInfoFields.userId === userId
      }
      _ <- allTokenInfo.map { case (token,_,_) =>
        forceExpire(token)
      }.sequence
    } yield true

}
