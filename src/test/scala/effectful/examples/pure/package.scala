package effectful.examples

import effectful._
import effectful.cats.CaptureTransform
import effectful.examples.pure.uuid.UUIDs

package object pure {
  implicit object LiftService_UUIDService extends LiftService[UUIDs] {

    override def apply[F[_], G[_]](
      s: UUIDs[F]
    )(implicit
      X: CaptureTransform[F,G]
    ) = {
      import UUIDs._
      new UUIDs[G] {
        override def gen() =
          X(s.gen())
        override def fromBase64(str: String) =
          s.fromBase64(str)
        override def toBase64(uuid: UUID) =
          s.toBase64(uuid)
        override def fromString(str: String) =
          s.fromString(str)
        override def toString(uuid: UUID) =
          s.toString(uuid)
      }
    }
  }
}
