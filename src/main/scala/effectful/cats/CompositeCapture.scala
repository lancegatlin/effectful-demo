package effectful.cats

trait CompositeCapture[F[_],G[_]] extends Capture[({ type FG[A] = F[G[A]]})#FG] {
  implicit val F:Capture[F]
  implicit val G:Capture[G] // note: Capture[G] might be based on Applicative[G]

  def capture[A](a: => A) =
    F.capture(G.capture(a))
}

object CompositeCapture {
  implicit def apply[F[_],G[_]](implicit
    F:Capture[F],
    G:Capture[G]
  ) = {
    val _F = F
    val _G = G
    new CompositeCapture[F,G] {
      implicit val F = _F
      implicit val G = _G
    }
  }
}
