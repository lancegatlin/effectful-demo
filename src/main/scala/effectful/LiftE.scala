package effectful

import scala.language.higherKinds

/**
  * A type-class for lifting the computation of an effect system monad into
  * another effect system's monad
  * @tparam E effect system monad
  * @tparam F a different effect system monad
  */
trait LiftE[E[+_],F[+_]] {
  /**
    * Lift the computation of an effect system monad into another
    * effect system monad.
    *
    * Note: E[A] must be lazy since not all effect systems capture effects (e.g. Id)
    * or capture effects in the same way (e.g. Future vs Task)
    * (which differs from natural transformation)
    *
    * @param ea computation of E[A]
    * @param E effect system type-class for E
    * @param F effect system type-class for F
    * @tparam A inner type of monad E
    * @return an instance of the other effect systems monad F that ensures capture
    *         of any leaked effects of E[A]
    */
  def apply[A](
    ea: => E[A]
  )(implicit
    E:EffectSystem[E],
    F:EffectSystem[F]
  ) : F[A]
}
