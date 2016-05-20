package effectful.examples.pure.user

import scala.language.higherKinds
import scalaz.\/
import effectful._

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
  
  implicit object LiftS_UserLoginService extends LiftS[UserLoginService] {

    override def apply[E[_], F[_]](
      s: UserLoginService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): UserLoginService[F] =
      new UserLoginService[F] {
        override def login(username: String, password: String): F[\/[LoginFailure, Token]] =
          liftE(s.login(username,password))
      }
  }
}
