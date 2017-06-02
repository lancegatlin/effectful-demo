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

  val eMonadMonadless = io.monadless.cats.MonadlessMonad[E]()
  import eMonadMonadless._

  override def issue(
    userId: UUID,
    deviceId: Option[UUID],
    expireAfter: Option[Duration]
  ): E[(Token,TokenInfo)] =
    lift {
      val uuid = unlift(uuids.gen())
      val token = uuid.toString
      val tokenInfo = TokenInfo(
        userId = userId,
        deviceId = deviceId,
        lastValidated = Instant.now(),
        expiresOn = Instant.now().plus(
          expireAfter.getOrElse(tokenDefaultDuration).toMillis,
          MILLIS
        )
      )
      val result = unlift(tokensDao.insert(token, tokenInfo))
      if(result == false) {
        unlift(E.failure(throw new RuntimeException("Failed to create token")))
      }
      unlift(info(s"Issued token $uuid to user $userId"))
      (token,tokenInfo)
    }

  override def validate(token: String): E[Option[TokenInfo]] =
  // todo: fix me
//    lift {
//      val optTokenInfo = unlift(tokensDao.findById(token))
//      optTokenInfo match {
//        case Some((_, tokenInfo, _)) if tokenInfo.expiresOn.compareTo(Instant.now()) > 0 =>
//          val result = unlift(tokensDao.update(token, tokenInfo.copy(
//            lastValidated = Instant.now()
//          )))
//          if (result) {
//            unlift(info(s"Validated token $token for user ${optTokenInfo.get._2.userId}"))
//          } else {
//            unlift(E.failure(throw new RuntimeException("Failed to update token")))
//          }
//          Some(tokenInfo)
//        case _ => None
//      }
//    }
    for {
      optTokenInfo <- tokensDao.findById(token)
      result <- optTokenInfo match {
        case Some((_, tokenInfo, _)) if tokenInfo.expiresOn.compareTo(Instant.now()) > 0 =>
          for {
            result <- tokensDao.update(token, tokenInfo.copy(
              lastValidated = Instant.now()
            ))
            _ <- {
              if (result) {
                info(s"Validated token $token for user ${optTokenInfo.get._2.userId}")
              } else {
                E.failure(throw new RuntimeException("Failed to update token"))
              }
            }: E[Unit]
          } yield Some(tokenInfo)
        case _ => E.pure(None)
      }
    } yield result

  override def find(token: Token): E[Option[TokenInfo]] =
    tokensDao.findById(token).map(_.map(_._2))

  override def forceExpire(token: Token): E[Unit] =
    lift {
      unlift(tokensDao.findById(token)) match {
        case Some((_,tokenInfo,_)) =>
          val result = unlift(tokensDao.update(token, tokenInfo.copy(
            expiresOn = Instant.now()
          )))
          if(result) {
            unlift(info(s"Forced expiration of token $token"))
          } else {
            unlift[Unit](E.failure(throw new RuntimeException("Failed to update token")))
          }
        case None =>
          unlift[Unit](E.failure(throw new RuntimeException("Token does not exist")))
      }
      ()
    }

  override def forceAllExpire(userId: UUID, exceptTokens: Token*): E[Boolean] =
    lift {
      val allTokenInfo = unlift(tokensDao.findByNativeQuery(sql"`user_id`=$userId"))
      unlift {
        allTokenInfo.map { case (token, _, _) =>
          forceExpire(token)
        }.sequence
      }
      unlift(info(s"Forced expiration of all tokens for $userId except ${exceptTokens.mkString(",")}"))
      true
    }
}
