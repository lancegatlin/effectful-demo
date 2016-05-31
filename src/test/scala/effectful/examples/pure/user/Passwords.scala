package effectful.examples.pure.user

trait Passwords[E[_]] {
  def compareDigest(
    d1: String,
    d2: String
  ) : E[Boolean]

  def mkDigest(plainText: String) : E[String]
}