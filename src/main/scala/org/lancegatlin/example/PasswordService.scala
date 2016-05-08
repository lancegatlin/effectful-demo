package org.lancegatlin.example

import org.lancegatlin.effectful.{EffectSystem, LiftE, LiftS}

import scala.language.higherKinds

trait PasswordService[E[_]] {
  def compareDigest(
    d1: String,
    d2: String
  ) : E[Boolean]

  def mkDigest(plainText: String) : E[String]
}

object PasswordService {
  implicit object LiftS_PasswordService extends LiftS[PasswordService] {
    override def apply[E[_], F[_]](
      s: PasswordService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): PasswordService[F] =
      new PasswordService[F] {
        override def mkDigest(plainText: String) =
          liftE(s.mkDigest(plainText))
        override def compareDigest(d1: String, d2: String) =
          liftE(s.compareDigest(d1,d2))
      }
  }
}