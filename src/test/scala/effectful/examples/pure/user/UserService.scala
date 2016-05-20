package effectful.examples.pure.user

import java.time.Instant

import effectful._
import effectful.examples.pure.UUIDService.UUID

import scala.language.higherKinds

trait UserService[E[_]] {
  import UserService._

  def findByUsername(username: String) : E[Option[User]]
  def findById(id: UUID) : E[Option[User]]
  def findAll(start: Int, batchSize: Int) : E[Seq[User]]
  def create(
    id: UUID,
    username: String,
    password: String
  ) : E[Boolean]
  def rename(userId: UUID, newUsername: String) : E[Boolean]
  def remove(userId: User) : E[Boolean]
 }

object UserService {
  case class User(
    id: UUID,
    username: String,
    passwordDigest: String,
    created: Instant,
    removed: Option[Instant]
  )
  
  implicit object LiftS_UserService extends LiftS[UserService] {

    override def apply[E[_], F[_]](
      s: UserService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): UserService[F] =
      new UserService[F] {
        override def findByUsername(username: String): F[Option[User]] =
          liftE(s.findByUsername(username))
        override def rename(userId: UUID, newUsername: String): F[Boolean] =
          liftE(s.rename(userId,newUsername))
        override def findById(id: UUID): F[Option[User]] =
          liftE(s.findById(id))
        override def findAll(start: Int, batchSize: Int): F[Seq[User]] =
          liftE(s.findAll(start,batchSize))
        override def remove(userId: User): F[Boolean] =
          liftE(s.remove(userId))
        override def create(id: UUID, username: String, password: String): F[Boolean] =
          liftE(s.create(id,username,password))
      }
  }
}
