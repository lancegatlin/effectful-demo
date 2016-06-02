package effectful.aspects
import effectful.cats.{Applicative, Monad}

import scala.concurrent.duration.FiniteDuration

trait CompositeDelay[F[_],G[_]] extends Delay[({ type FG[A] = F[G[A]]})#FG] {
  val D:Delay[F]
  implicit val F:Monad[F]
  implicit val G:Applicative[G]

  def delay(duration: FiniteDuration) = {
    import Monad.ops._

    D.delay(duration).map(_ => G.pure(()))
  }
}

object CompositeDelay {
  implicit def apply[F[_],G[_]](implicit
    D:Delay[F],
    F:Monad[F],
    G:Applicative[G]
  ) = {
    val _D = D
    val _F = F
    val _G = G
    new CompositeDelay[F,G] {
      override val D = _D
      override val F = _F
      override val G = _G
    }
  }
}