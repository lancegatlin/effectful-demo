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
