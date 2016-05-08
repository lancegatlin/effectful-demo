package org.lancegatlin.example

import scala.language.higherKinds

trait UserService[E[_]] extends EntityService[UUID,UserService.User,E] {
  import UserService._

  def findByUsername(username: String) : E[Option[User]]
}

object UserService {
  case class User(
    username: String,
    passwordDigest: String
  )
}
