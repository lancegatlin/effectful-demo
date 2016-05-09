package effectful

import scala.language.higherKinds

/**
  * A type-class for lifting the effect system of a service into another
  * effect system
  *
  * @tparam S the service
  */
trait LiftS[S[_[_]]] {
  /**
    * Create a new instance of a service that returns computations
    * in a different effect system by utilizing the supplied service and
    * its current effect system
    *
    * @param s service to lift
    * @param E a type-class for service's effect system
    * @param F a type-class for the new effect system to lift into
    * @param liftE a type-class for lifting from the effect system E into F
    * @tparam E service's effect system
    * @tparam F new effect system to lift into
    * @return an instance of S[F] that utilizes the underlying S[E] to compute
    *         values by lifting all computed E[_] values into F[_]
    */
  def apply[E[_],F[_]](
    s: S[E]
  )(implicit
    E:EffectSystem[E],
    F:EffectSystem[F],
    liftE:LiftE[E,F]
  ) : S[F]
}
