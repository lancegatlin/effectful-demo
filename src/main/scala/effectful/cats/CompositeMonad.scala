package effectful.cats

object CompositeMonad {
  def apply[F[_],G[_]](implicit
    F:Monad[F],
    G:Monad[G],
    flatSequenceFG: FlatSequence[F,G]
  ) : Monad[({ type FG[A] = F[G[A]]})#FG] =
    new Monad[({ type FG[A] = F[G[A]]})#FG] {
      import flatSequenceFG._

      def map[A, B](fga: F[G[A]])(f: (A) => B) =
        F.map(fga)(ga => G.map(ga)(f))
      def flatMap[A, B](fga: F[G[A]])(f: (A) => F[G[B]]) =
        F.flatMap(fga)(ga => flatSequence(ga)(f))
      def widen[A, AA >: A](fga: F[G[A]]) =
        F.map(fga)(ga => G.widen(ga))
      def pure[A](a: A) =
        F.pure(G.pure(a))
    }
}
