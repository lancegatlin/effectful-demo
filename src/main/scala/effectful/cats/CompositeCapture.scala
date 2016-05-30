package effectful.cats

object CompositeCapture {
  def apply[F[_],G[_]](implicit
    F:Capture[F] with Applicative[F],
    G:Capture[G]
  ) : Capture[({ type FG[A] = F[G[A]]})#FG] =
    new Capture[({ type FG[A] = F[G[A]]})#FG] {
      def capture[A](a: => A) =
        F.pure(G.capture(a))
    }
}
