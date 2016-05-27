package effectful.aspects

import scala.concurrent.duration.FiniteDuration

trait Delay[E[_]] {
  def delay(duration: FiniteDuration) : E[Unit]
}
