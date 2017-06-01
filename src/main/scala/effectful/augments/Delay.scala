package effectful.augments

import cats._

import scala.concurrent.duration.FiniteDuration

trait Delay[E[_]] {
  def delay(duration: FiniteDuration) : E[Unit]
}

object Delay {
  implicit def apply[F[_],G[_]](implicit
    D:Delay[F],
    F:Monad[F],
    G:Applicative[G]
  ) : Delay[({ type FG[A] = F[G[A]]})#FG] =
    CompositeDelay[F,G]
}