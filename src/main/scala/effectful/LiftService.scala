package effectful

/**
  * A type-class for lifting the exec monad of a service into another
  * exec monad
  *
  * @tparam S the type of the service whose methods are all wrapped in
  *           an exec monad
  */
trait LiftService[S[_[_]]] {
  /**
    * Create a new instance of a service that returns computations
    * in a different exec monad by utilizing the supplied service and
    * its current exec monad
    *
    * @param s service to lift
    * @param liftCapture a type-class for lifting from E into F
    * @tparam E type of service's exec monad
    * @tparam F type of target exec monad
    * @return an instance of S[F] that utilizes the underlying S[E] to compute
    *         values by lifting all computed E[_] values into F[_]
    */
  def apply[E[_],F[_]](
    s: S[E]
  )(implicit
    liftCapture:LiftCapture[E,F]
  ) : S[F]
}
