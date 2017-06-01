package effectful.examples.effects

import effectful._
import effectful.augments.CaptureTransform

package object logging {
  implicit object LiftService_Logger extends LiftService[Logger] {
    override def apply[F[_], G[_]](
      s: Logger[F]
    )(implicit
      X: CaptureTransform[F,G]
    ) =
      new Logger[G] {
        override def trace(message: =>String) =
          X(s.trace(message))
        override def warn(message: =>String) =
          X(s.warn(message))
        override def warn(message: =>String, cause: Throwable) =
          X(s.warn(message,cause))
        override def error(message: =>String) =
          X(s.error(message))
        override def error(message: =>String, cause: Throwable) =
          X(s.error(message,cause))
        override def debug(message: =>String) =
          X(s.debug(message))
        override def debug(message: =>String, cause: Throwable) =
          X(s.debug(message))
        override def trace(message: =>String, cause: Throwable) =
          X(s.trace(message,cause))
        override def info(message: =>String) =
          X(s.info(message))
        override def info(message: => String, cause: Throwable) =
          X(s.info(message,cause))
      }
  }
}
