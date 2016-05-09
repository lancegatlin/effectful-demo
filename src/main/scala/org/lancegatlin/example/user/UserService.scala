package org.lancegatlin.example.user

import scala.language.higherKinds
import org.lancegatlin.example.UUID

trait UserService[E[_]] {
  import UserService._

  def findByUsername(username: String) : E[Option[User]]
  def findById(id: UUID) : E[Option[User]]
  def findAll(start: Int, batchSize: Int) : E[Seq[User]]
  def create(user: User) : E[Boolean]
  def rename(userId: UUID, newUsername: String) : E[Boolean]
  def remove(userId: User) : E[Boolean]
 }

object UserService {
  case class User(
    id: UUID,
    username: String,
    passwordDigest: String
  )
}
