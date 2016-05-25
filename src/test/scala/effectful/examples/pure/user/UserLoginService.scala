package effectful.examples.pure.user

import scalaz.\/

trait UserLoginService[E[_]] {
  import UserLoginService._

  def login(username: String, password: String) : E[LoginFailure \/ Token]
}

object UserLoginService {
  type Token = String
  sealed trait LoginFailure
  object LoginFailure {
    case object PasswordMismatch extends LoginFailure
    case object UserDoesNotExist extends LoginFailure
  }
}
