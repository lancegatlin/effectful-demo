package effectful.examples.pure.user

trait Passwords[E[_]] {
  def compareDigest(
    plainTextPassword: String,
    passwordDigest: String
  ) : E[Boolean]

  def mkDigest(plainTextPassword: String) : E[String]
}