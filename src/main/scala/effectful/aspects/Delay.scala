package effectful.aspects

import effectful.cats.Capture
import effectful.impl.BlockingDelay

import scala.concurrent.duration.FiniteDuration

trait Delay[E[_]] {
  def delay(duration: FiniteDuration) : E[Unit]
}

object Delay {
  implicit def mkDelay[E[_]](implicit
    E:Capture[E]
  ) : Delay[E] = {
    val _E = E
    new BlockingDelay[E] {
      implicit val E = _E
    }
  }
}