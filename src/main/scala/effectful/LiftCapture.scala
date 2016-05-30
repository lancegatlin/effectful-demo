package effectful

import effectful.cats.Capture

/**
  * A type-class for lifting the computation of an exec monad into
  * another exec monad
 *
  * @tparam E effect system monad
  * @tparam F a different effect system monad
  */
trait LiftCapture[E[_],F[_]] {
  /**
    * Lift the computation of an exec monad into another exec monad.
    *
    * Note: E[A] must be lazy since not all monads capture effects (e.g. Id)
    * or capture effects in the same way (e.g. Future vs Task)
    * (which means LiftE differs from natural transformation)
    *
    * @param ea computation of E[A]
    * @param E type-class for E
    * @param F type-class for F
    * @tparam A inner type of monad E
    * @return an instance of the other monad F that ensures capture
    *         of any leaked effects of E[A]
    */
  def apply[A](
    ea: => E[A]
  )(implicit
    E:Capture[E],
    F:Capture[F]
  ) : F[A]
}
