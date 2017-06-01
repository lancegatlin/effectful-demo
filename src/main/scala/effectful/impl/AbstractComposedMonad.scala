package effectful.impl

import cats._

abstract class AbstractComposedMonad[F[_],G[_]](implicit
  F: Monad[F],
  G: Monad[G],
  GT: Traverse[G]
) extends Monad[({ type FG[A] = F[G[A]]})#FG] {
  def pure[A](x: A) = F.pure(G.pure(x))

  override def map[A, B](fa: F[G[A]])(f: (A) => B) =
    F.map(fa)(G.map(_)(f))

  def flatMap[A, B](fa: F[G[A]])(f: (A) => F[G[B]]) : F[G[B]] =
    F.flatMap(fa) { ga =>
      GT.flatTraverse(ga)(f)
    }

  protected def notStackSafeM[A, B](a: A)(f: (A) => F[G[Either[A, B]]]) : F[G[B]] =
    flatMap(f(a)) {
      case Right(b) => pure(b)
      case Left(aa) => tailRecM(aa)(f)
    }
}

