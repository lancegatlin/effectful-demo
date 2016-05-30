package effectful.cats

trait Applicative[E[_]] {
  /**
    * Lift an already computed value into E
    * @param a computed value
    * @return an instance of E in the context of E
    */
  def pure[A](a: A) : E[A]
}
