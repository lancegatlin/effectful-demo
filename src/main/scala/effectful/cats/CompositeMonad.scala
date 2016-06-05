package effectful.cats

trait CompositeMonad[F[_],G[_]] extends Monad[({ type FG[A] = F[G[A]]})#FG] {
  val F:Monad[F]
  val G:Monad[G]

  def map[A, B](fga: F[G[A]])(f: (A) => B) =
    F.map(fga)(ga => G.map(ga)(f))
  def flatMap[A, B](fga: F[G[A]])(f: (A) => F[G[B]]) =
    F.flatMap(fga)(ga => flatSequence(ga)(f))
  def widen[A, AA >: A](fga: F[G[A]]) =
    F.map(fga)(ga => G.widen(ga))
  def pure[A](a: A) =
    F.pure(G.pure(a))

  def flatSequence[A,B](ga: G[A])(f: A => F[G[B]]) : F[G[B]]
}

object CompositeMonad {
  implicit def apply[F[_],G[_]](implicit
    F:Monad[F],
    G:Monad[G],
    flatSequenceFG: FlatSequence[F,G]
  ) = {
    val _F = F
    val _G = G
    new CompositeMonad[F,G] {
      val F = _F
      val G = _G
      def flatSequence[A, B](ga: G[A])(f: (A) => F[G[B]]) =
        flatSequenceFG.flatSequence(ga)(f)
    }
  }
}
