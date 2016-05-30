package effectful.examples

import effectful._

package object pure {
  implicit object LiftService_UUIDService extends LiftService[UUIDService] {

    override def apply[E[_], F[_]](
      s: UUIDService[E]
    )(implicit
      liftCapture: LiftCapture[E, F]
    ): UUIDService[F] = {
      import UUIDService._
      new UUIDService[F] {
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
