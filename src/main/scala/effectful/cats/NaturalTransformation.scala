package effectful.cats

trait NaturalTransformation[F[_],G[_]] {
  def apply[A](f: F[A]) : G[A]
}

//object NaturalTransformation {
//  implicit def apply[F[_],G[_]](implicit
//    G:Applicative[G]
//  ) : NaturalTransformation[F,({ type GF[A] = G[F[A]]})#GF] =
//    new NaturalTransformation[F,({ type GF[A] = G[F[A]]})#GF] {
//      override def apply[A](f: F[A]): G[F[A]] =
//        G.pure(f)
//    }
//}