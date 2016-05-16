package effectful.examples.pure.user

import scala.language.higherKinds

trait PasswordService[E[+_]] {
  def compareDigest(
    d1: String,
    d2: String
  ) : E[Boolean]

  def mkDigest(plainText: String) : E[String]
}