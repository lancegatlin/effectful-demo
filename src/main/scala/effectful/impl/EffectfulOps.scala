package effectful.impl

import scala.collection.generic.CanBuildFrom
import effectful.cats.Monad
import effectful._

object EffectfulOps {
  def sequence[M[_],A,F[AA] <: Traversable[AA]](self: F[M[A]])(implicit
    M: Monad[M],
    cbf: CanBuildFrom[Nothing, A, F[A]]
  ) : M[F[A]] = {
    self.foldLeft(M(cbf())) { (mBuilder,ea) =>
      for {
        builder <- mBuilder
        a <- ea
      } yield builder += a
    }.map(_.result())
  }
}
