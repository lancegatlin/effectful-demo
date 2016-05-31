package effectful.examples.pure.user

import scalaz.\/

trait UserLogins[E[_]] {
  import UserLogins._

  def login(username: String, password: String) : E[LoginFailure \/ Token]
}

object UserLogins {
  type Token = String
  sealed trait LoginFailure
  object LoginFailure {
    case object PasswordMismatch extends LoginFailure
    case object UserDoesNotExist extends LoginFailure
  }
}
