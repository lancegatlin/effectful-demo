package effectful.impl

import scala.concurrent.duration.FiniteDuration
import effectful.augments.{Capture, Delay}

trait BlockingDelay[E[_]] extends Delay[E] {
  implicit val E:Capture[E]
  override def delay(duration: FiniteDuration): E[Unit] =
    E.capture {
      Thread.sleep(duration.toMillis)
    }
}
