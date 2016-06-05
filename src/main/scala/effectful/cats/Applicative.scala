package effectful.cats

trait Applicative[E[_]] {
  /**
    * Lift an already computed value into E
    * @param a computed value
    * @return an instance of E in the context of E
    */
  def pure[A](a: A) : E[A]
}

object Applicative {
  object ops extends ops
  trait ops {

  }
//  implicit def apply[F[_],G[_]](implicit
//    F:Applicative[F],
//    G:Applicative[G]
//  ) : Applicative[({ type FG[A] = F[G[A]]})#FG] =
//    CompositeApplicative[F,G]
}