package effectful.examples.effects.delay

import effectful.{EffectSystem, LiftE, LiftS}

import scala.concurrent.duration.FiniteDuration

trait DelayService[E[_]] {
  def delay(duration: FiniteDuration) : E[Unit]
}

object DelayService {
  implicit object liftS_DelayService extends LiftS[DelayService] {
    override def apply[E[_], F[_]](
      s: DelayService[E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): DelayService[F] =
      new DelayService[F] {
        override def delay(duration: FiniteDuration): F[Unit] =
          liftE(s.delay(duration))
      }
  }
}
