package effectful.impl

import scala.collection.generic.CanBuildFrom
import cats._

object EffectfulOps {
  def sequence[M[_],A,F[AA] <: Traversable[AA]](self: F[M[A]])(implicit
    M: Monad[M],
    cbf: CanBuildFrom[Nothing, A, F[A]]
  ) : M[F[A]] = {
    import Monad.ops._

    self.foldLeft(M.pure(cbf())) { (mBuilder,ea) =>
      for {
        builder <- mBuilder
        a <- ea
      } yield builder += a
    }.map(_.result())
  }
}
