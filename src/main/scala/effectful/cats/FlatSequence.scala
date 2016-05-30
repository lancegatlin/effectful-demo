package effectful.cats

trait FlatSequence[F[_],G[_]] {
  def flatSequence[A,B](ga: G[A])(f: A => F[G[B]]) : F[G[B]]
}
