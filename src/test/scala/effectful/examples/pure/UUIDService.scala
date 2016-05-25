package effectful.examples.pure

trait UUIDService[E[_]] {
  import UUIDService._

  def gen() : E[UUID]
  def toString(uuid: UUID) : String
  def fromString(s: String) : Option[UUID]
  def toBase64(uuid: UUID) : String
  def fromBase64(s: String) : Option[UUID]
}

object UUIDService {
  case class UUID(bytes: Array[Byte])
}