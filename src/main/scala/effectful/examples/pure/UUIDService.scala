package effectful.examples.pure

import scala.language.higherKinds

trait UUIDService[E[_]] {
  import UUIDService._

  def gen() : E[UUID]
  def toString(uuid: UUID) : String
  def fromString(s: String) : E[Option[UUID]]
  def toBase64(uuid: UUID) : String
  def fromBase64(s: String) : UUID
}

object UUIDService {
  case class UUID(bytes: Array[Byte])
}