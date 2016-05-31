package effectful.examples.pure.user

import java.time.Instant
import effectful.examples.pure.uuid.UUIDService.UUID

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
  def remove(userId: UUID) : E[Boolean]
 }

object UserService {
  case class User(
    id: UUID,
    username: String,
    passwordDigest: String,
    created: Instant,
    removed: Option[Instant]
  )
}
