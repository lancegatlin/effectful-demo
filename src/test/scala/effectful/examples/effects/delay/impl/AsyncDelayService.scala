package effectful.examples.effects.delay.impl

import effectful.examples.effects.delay.DelayService
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import s_mach.concurrent._

class AsyncDelayService()(implicit
  scheduledExecutionContext:ScheduledExecutionContext
) extends DelayService[Future] {
  def delay(duration: FiniteDuration) = {
    Future.delayed(duration)(())
  }
}
