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
//  import Monad.ops._
  import UsersImpl._
  import logger._
  val eMonadMonadless = io.monadless.cats.MonadlessMonad[E]()
  import eMonadMonadless._

  def create(id: UUID, username: String, plainTextPassword: String) =
    lift {
      unlift(findById(id)) match {
        case Some(_) => false
        case None =>
          unlift(findByUsername(username)) match {
            case Some(_) => false
            case None =>
              val digest = unlift(passwords.mkDigest(plainTextPassword))
              val result = unlift(usersDao.insert(id,UserData(
                username = username,
                passwordDigest = digest
              )))
              if(result) {
                unlift(info(s"Created user $id with username $username"))
              }
              result
          }
      }
    }

  def findById(id: UUID) =
    lift {
      unlift(usersDao.findById(id))
        .map(toUser)
    }

  def findByUsername(username: String) =
    lift {
      unlift(usersDao.findByNativeQuery(sql"`username`=$username"))
        .headOption
        .map(toUser)
    }


  def findAll(start: Int, batchSize: Int) =
    lift {
      unlift(usersDao.findAll(start, batchSize))
        .map(toUser)
    }


  def remove(userId: UUID) =
    usersDao.remove(userId)

  def rename(userId: UUID, newUsername: String) =
    lift {
      unlift(usersDao.findById(userId)) match {
        case Some((_,userData,_)) =>
          val maybeUser = unlift(findByUsername(newUsername))
          val result = if(maybeUser.isEmpty) {
            unlift(usersDao.update(userId,userData.copy(username = newUsername)))
          } else {
            false
          }
          if(result) {
            unlift(info(s"Renamed user $userId to $newUsername"))
          }
          result
        case None => false
      }
    }

  override def setPassword(userId: UUID, plainTextPassword: String): E[Boolean] =
    lift {
      unlift(usersDao.findById(userId)) match {
        case Some((_,userData,_)) =>
          val newDigest = unlift(passwords.mkDigest(plainTextPassword))
          val result = unlift(usersDao.update(userId,userData.copy(passwordDigest = newDigest)))
          if(result) {
            unlift(info(s"Changed password for user $userId"))
          }
          result
        case None => false
      }
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
