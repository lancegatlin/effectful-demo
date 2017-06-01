package effectful.examples.pure.user.impl

import java.time.Instant
import java.time.temporal.ChronoUnit.MILLIS

import scala.concurrent.duration.Duration
import effectful._
import effectful.augments._
import cats.Monad
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.dao.sql._
import effectful.examples.pure.user.Tokens
import effectful.examples.pure.uuid.UUIDs
import effectful.examples.pure.uuid.UUIDs.UUID

class TokensImpl[E[_]](
  uuids: UUIDs[E],
  tokensDao: SqlDocDao[String,Tokens.TokenInfo,E],
  tokenDefaultDuration: Duration,
  logger: Logger[E]
)(implicit
  E:Monad[E],
  X:Exceptions[E],
  sqlPrint_UUID: PrintSql[UUID]
) extends Tokens[E] {
  import Monad.ops._
  import Tokens._
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
      result <- tokensDao.insert(token, tokenInfo)
      _ <- {
        if(result == false) {
          E.failure(throw new RuntimeException("Failed to create token"))
        } else {
          E.pure(())
        }
      }:E[Unit] // Note: fix intellij erroneous error
      _ <- info(s"Issued token $uuid to user $userId")
    } yield (token,tokenInfo)


  override def validate(token: String): E[Option[TokenInfo]] =
    for {
      optTokenInfo <- tokensDao.findById(token)
      result <- optTokenInfo match {
        case Some((_,tokenInfo,_)) if tokenInfo.expiresOn.compareTo(Instant.now()) > 0 =>
          for {
            result <- tokensDao.update(token, tokenInfo.copy(
              lastValidated = Instant.now()
            ))
            _ <- {
              if(result) {
                info(s"Validated token $token for user ${optTokenInfo.get._2.userId}")
              } else {
                E.failure(throw new RuntimeException("Failed to update token"))
              }
            }:E[Unit]
          } yield Some(tokenInfo)
        case _ => E.pure(None)
      }
    } yield result

  override def find(token: Token): E[Option[TokenInfo]] =
    tokensDao.findById(token).map(_.map(_._2))

  override def forceExpire(token: Token): E[Unit] =
    for {
      optTokenInfo <- tokensDao.findById(token)
      result <- {
        optTokenInfo match {
          case Some((_,tokenInfo,_)) =>
            tokensDao.update(token, tokenInfo.copy(
              expiresOn = Instant.now()
            ))
          case None => E.failure(throw new RuntimeException("Token does not exist"))
        }
      }:E[Boolean] // Note: fix intellij erroneous error
      _ <- {
        if(result) {
          info(s"Forced expiration of token $token")
        } else {
          E.failure(throw new RuntimeException("Failed to update token"))
        }
      }
    } yield ()

  override def forceAllExpire(userId: UUID, exceptTokens: Token*): E[Boolean] =
    for {
      allTokenInfo <- tokensDao.findByNativeQuery(sql"`user_id`=$userId")
      _ <- allTokenInfo.map { case (token,_,_) =>
        forceExpire(token)
      }.sequence
      _ <- info(s"Forced expiration of all tokens for $userId except ${exceptTokens.mkString(",")}")
    } yield true

}
