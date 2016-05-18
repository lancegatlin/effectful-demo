package effectful.examples.effects.delay

import scala.language.higherKinds
import scala.concurrent.duration.FiniteDuration

trait DelayService[E[_]] {
  def delay(duration: FiniteDuration) : E[Unit]
}
