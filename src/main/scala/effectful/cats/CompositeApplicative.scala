package effectful.cats

trait CompositeApplicative[F[_],G[_]] extends Applicative[({ type FG[A] = F[G[A]]})#FG] {
  val F:Applicative[F]
  val G:Applicative[G]

  override def pure[A](a: A): F[G[A]] =
    F.pure(G.pure(a))
}

object CompositeApplicative {
  implicit def apply[F[_],G[_]](implicit
    F:Applicative[F],
    G:Applicative[G]
  ) = {
    val _F = F
    val _G = G
    new CompositeApplicative[F,G] {
      override val F = _F
      override val G = _G
    }
  }
}
