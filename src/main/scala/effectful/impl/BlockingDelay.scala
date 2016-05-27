package effectful.impl

import scala.concurrent.duration.FiniteDuration
import effectful._
import effectful.aspects.Delay

trait BlockingDelay[E[_]] extends Delay[E] {
  implicit val E:Exec[E]
  override def delay(duration: FiniteDuration): E[Unit] =
    E(Thread.sleep(duration.toMillis))
}
