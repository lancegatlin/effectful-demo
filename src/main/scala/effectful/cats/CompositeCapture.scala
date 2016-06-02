package effectful.cats

trait CompositeCapture[F[_],G[_]] extends Capture[({ type FG[A] = F[G[A]]})#FG] {
  implicit val C:Capture[F]
  // todo: couldn't this be Capture[G] OR Applicative[G]?
  implicit val G:Capture[G]

  def capture[A](a: => A) =
    C.capture(G.capture(a))
}

object CompositeCapture {
  def apply[F[_],G[_]](implicit
    C:Capture[F],
    G:Capture[G]
  ) : Capture[({ type FG[A] = F[G[A]]})#FG] = {
    val _C = C
    val _G = G
    new CompositeCapture[F,G] {
      implicit val C = _C
      implicit val G = _G
    }
  }
}
