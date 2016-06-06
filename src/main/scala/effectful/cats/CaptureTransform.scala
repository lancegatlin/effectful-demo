package effectful.cats

// combine capture, natural transformation & monad
trait CaptureTransform[F[_],G[_]] {
  def apply[A](f: => F[A]) : G[A]
}

//object CaptureTransform {
//  implicit def apply[F[_],G[_]](implicit
//    C:Capture[G],
//    G:Monad[G],
//    K:NaturalTransformation[F,G]
//  ) : CaptureTransform[F,G] =
//    new CaptureTransform[F,G] {
//      override def apply[A](f: => F[A]): G[A] = {
//        G.flatMap(C.capture(K(f)))(identity)
//      }
//    }
//}
