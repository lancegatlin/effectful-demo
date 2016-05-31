package effectful.examples

import effectful._
import effectful.examples.pure.uuid.UUIDs

package object pure {
  implicit object LiftService_UUIDService extends LiftService[UUIDs] {

    override def apply[E[_], F[_]](
      s: UUIDs[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): UUIDs[F] = {
      import UUIDs._
      new UUIDs[F] {
        override def gen(): F[UUID] =
          liftCapture(s.gen())
        override def fromBase64(str: String): Option[UUID] =
          s.fromBase64(str)
        override def toBase64(uuid: UUID): String =
          s.toBase64(uuid)
        override def fromString(str: String): Option[UUID] =
          s.fromString(str)
        override def toString(uuid: UUID): String =
          s.toString(uuid)
      }
    }
  }
}
