package effectful.cats

trait CompositeCapture[F[_],G[_]] extends Capture[({ type FG[A] = F[G[A]]})#FG] {
  implicit val C:Capture[F]
  // todo: couldn't this be Capture[G] OR Applicative[G]?
  implicit val G:Applicative[G]

  def capture[A](a: => A) =
    C.capture(maybeCapture(a))

  def maybeCapture[A](a: => A) : G[A]
}

object CompositeCapture {
  def apply[F[_],G[_]](implicit
    C:Capture[F],
    G:Applicative[G]
  ) : Capture[({ type FG[A] = F[G[A]]})#FG] = {
    val _C = C
    val _G = G
    new CompositeCapture[F,G] {
      implicit val C = _C
      implicit val G = _G
      def maybeCapture[A](a: => A) =
        G.pure(a)
    }
  }

//  def apply[F[_],G[_]](implicit
//    F:Capture[F],
//    G:Capture[G]
//  ) : Capture[({ type FG[A] = F[G[A]]})#FG] = {
//    val _F = F
//    val _G = G
//    new CompositeCapture[F,G] {
//      implicit val F = _F
//      implicit val G = _G
//      def maybeCapture[A](a: => A) =
//        G.capture(a)
//    }
//  }
}
