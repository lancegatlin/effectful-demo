package effectful.examples.pure.user.impl

import cats.Monad
import effectful.examples.effects.logging.Logger
import effectful.examples.pure.dao.sql._
import effectful.examples.pure.dao.DocDao.RecordMetadata
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.user.{Passwords, Users}
import effectful.examples.pure.user.Users.User
import effectful.examples.pure.user.impl.UsersImpl.UserData
import effectful.examples.pure.uuid.UUIDs.UUID

object UsersImpl {
  case class UserData(
    username: String,
    passwordDigest: String
  )
}

class UsersImpl[E[_]](
  usersDao: SqlDocDao[UUID,UserData,E],
  passwords: Passwords[E],
  logger: Logger[E]
)(implicit
  E:Monad[E]
) extends Users[E] {
  import Monad.ops._
  import UsersImpl._
  import logger._

  def create(id: UUID, username: String, plainTextPassword: String) =
    findById(id).flatMap {
      case Some(_) => E.pure(false)
      case None =>
        findByUsername(username).flatMap {
          case Some(_) => E.pure(false)
          case None =>
            for {
              digest <- passwords.mkDigest(plainTextPassword)
              result <- usersDao.insert(id,UserData(
                username = username,
                passwordDigest = digest
              ))
              _ <- if(result) {
                info(s"Created user $id with username $username")
              } else {
                E.pure(())
              }
            } yield result
        }
    }

  def findById(id: UUID) =
    usersDao.findById(id).map(_.map(toUser))

  def findByUsername(username: String) =
    usersDao.findByNativeQuery(sql"`username`=$username").map(_.headOption.map(toUser))

  def findAll(start: Int, batchSize: Int) =
    usersDao.findAll(start, batchSize).map(_.map(toUser))

  def remove(userId: UUID) =
    usersDao.remove(userId)

  def rename(userId: UUID, newUsername: String) =
    usersDao.findById(userId).flatMap {
      case Some((_,userData,_)) =>
        for {
          maybeUser <- findByUsername(newUsername)
          result <- {
            if(maybeUser.isEmpty) {
              usersDao.update(userId,userData.copy(username = newUsername))
            } else {
              E.pure(false)
            }
          }
          _ <- if(result) {
            info(s"Renamed user $userId to $newUsername")
          } else {
            E.pure(())
          }
        } yield result
      case None =>
        E.pure(false)
    }


  override def setPassword(userId: UUID, plainTextPassword: String): E[Boolean] =
    usersDao.findById(userId).flatMap {
      case Some((_,userData,_)) =>
        for {
          newDigest <- passwords.mkDigest(plainTextPassword)
          result <- usersDao.update(userId,userData.copy(passwordDigest = newDigest))
          _ <- if(result) {
            info(s"Change password for user $userId")
          } else {
            E.pure(())
          }
        } yield result
      case None =>
        E.pure(false)
    }

  val toUser : ((UUID,UserData,RecordMetadata)) => User = { case (id,userData,metadata) =>
      User(
        id = id,
        username = userData.username,
        passwordDigest = userData.passwordDigest,
        created = metadata.created,
        removed = metadata.removed
      )
    }
}
