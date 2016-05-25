package effectful.examples

import effectful._

package object pure {
  implicit object LiftS_UUIDService extends LiftS[UUIDService] {

    override def apply[E[_], F[_]](
      s: UUIDService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): UUIDService[F] = {
      import UUIDService._
      new UUIDService[F] {
        override def gen(): F[UUID] =
          liftE(s.gen())
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