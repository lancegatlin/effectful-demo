package effectful.examples.effects.delay.impl

import effectful._
import effectful.examples.effects.delay.DelayService

import scala.concurrent.duration.FiniteDuration

class BlockingDelayService extends DelayService[Id] {
  override def delay(duration: FiniteDuration): Id[Unit] =
    Thread.sleep(duration.toMillis)
}
