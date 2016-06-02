package effectful.examples.pure

import effectful._
import effectful.cats.CaptureTransform
import effectful.examples.pure.uuid.UUIDs.UUID

import scala.concurrent.duration.Duration

package object user {
  implicit object LiftService_PasswordService extends LiftService[Passwords] {
    override def apply[F[_],G[_]](
      s: Passwords[F]
    )(implicit
      X:CaptureTransform[F,G]
    ) =
      new Passwords[G] {
        override def mkDigest(plainText: String) =
          X(s.mkDigest(plainText))
        override def compareDigest(d1: String, d2: String) =
          X(s.compareDigest(d1,d2))
      }
  }

  implicit object LiftService_TokenService extends LiftService[Tokens] {

    override def apply[F[_],G[_]](
      s: Tokens[F]
    )(implicit
      X:CaptureTransform[F,G]
    ) = {
      import Tokens._
      new Tokens[G] {
        override def issue(userId: UUID, deviceId: Option[UUID], expireAfter: Option[Duration]) =
          X(s.issue(userId,deviceId,expireAfter))
        override def validate(token: Token) =
          X(s.validate(token))
        override def forceAllExpire(userId: UUID, exceptTokens: Token*) =
          X(s.forceAllExpire(userId,exceptTokens:_*))
        override def find(token: Token) =
          X(s.find(token))
        override def forceExpire(token: Token) =
          X(s.forceExpire(token))
      }
    }
  }

  implicit object LiftService_UserLoginService extends LiftService[UserLogins] {
    override def apply[F[_],G[_]](
      s: UserLogins[F]
    )(implicit
      X:CaptureTransform[F,G]
    ) = {
      import UserLogins._
      new UserLogins[G] {
        override def login(username: String, password: String) =
          X(s.login(username,password))
      }
    }
  }
}
