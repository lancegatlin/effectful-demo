package effectful.examples.pure.uuid.free

import effectful.examples.pure.uuid.UUIDs.UUID

sealed trait UUIDCmd[A]
object UUIDCmd {
  case class Gen() extends UUIDCmd[UUID]
}
