package effectful.impl

import scala.concurrent.duration.FiniteDuration
import effectful.aspects.Delay
import effectful.cats.Capture

trait BlockingDelay[E[_]] extends Delay[E] {
  implicit val E:Capture[E]
  override def delay(duration: FiniteDuration): E[Unit] =
    E.capture {
      Thread.sleep(duration.toMillis)
    }
}

object BlockingDelay {
  def apply[E[_]](implicit
    E:Capture[E]
  ) : Delay[E] = {
    val _E = E
    new BlockingDelay[E] {
      implicit val E = _E
    }
  }
}