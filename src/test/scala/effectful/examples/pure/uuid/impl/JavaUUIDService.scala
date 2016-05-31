package effectful.examples.pure.uuid.impl

import java.nio.ByteBuffer
import java.util.{UUID => JavaUUID}

import effectful._
import effectful.examples.pure.uuid.UUIDService
import org.apache.commons.codec.binary.Base64

import scala.util.Try

class JavaUUIDService extends UUIDService[Id] {
  import UUIDService._

  def toUUID(uuid: JavaUUID) = UUID(toBytes(uuid))

  def toBytes(uuid: JavaUUID) : Array[Byte] = {
    val bb = ByteBuffer.allocate(16)
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    bb.array()
  }

  def toJavaUUID(uuid: UUID) : JavaUUID = {
    val bb = ByteBuffer.wrap(uuid.bytes.toArray)
    val msb = bb.getLong
    val lsb = bb.getLong
    new JavaUUID(msb,lsb)
  }

  override def gen(): Id[UUID] =
    toUUID(JavaUUID.randomUUID())


  override def toString(uuid: UUID): String =
    toJavaUUID(uuid).toString

  override def fromBase64(s: String): Option[UUID] = {
    val uuid = UUID(Base64.decodeBase64(s))
    Try(toJavaUUID(uuid)).toOption.map(_ => uuid)
  }

  override def toBase64(uuid: UUID): String =
    Base64.encodeBase64URLSafeString(uuid.bytes.toArray)

  override def fromString(s: String): Id[Option[UUID]] =
    Try(JavaUUID.fromString(s)).toOption.map(toUUID)
}
