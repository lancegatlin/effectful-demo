
import scala.collection.generic.CanBuildFrom
import effectful.cats._
import effectful.aspects._

package object effectful {
  /**
    * Identity monad
    */
  type Id[A] = A

  // todo: figure out how this sugar is declared in emm
//  type |:[F[_],G[_]] = F[G[_]]
//  val |: = Nested

  type Exec[E[_]] =
    Capture[E] with
    Monad[E] with
    Delay[E] with
    Par[E] with
    Exceptions[E]

  /**
    * Add the sequence method to any Collection of a effect system's monad
    * that simply forwards the call to the implicit EffectSystem type-class
    *
    * @param self collection of effects
    * @tparam F collection type
    * @tparam A type contained in collection
    */
  implicit class SequenceOpsPML[
    M[_],
    F[AA] <: Traversable[AA],
    A
  ](
    val self: F[M[A]]
  ) extends AnyVal {
    /**
      * Sequence a collection of effects into an effect of the collection
      *
      * Note: this method is unnecessary if using scalaz and is here for
      * compatibility if not using scalaz
      *
      * @return
      */
    def sequence(implicit
      M: Monad[M],
      cbf: CanBuildFrom[Nothing, A, F[A]]
    ) : M[F[A]] =
      impl.EffectfulOps.sequence[M,A,F](self)
  }

  implicit object capture_Id extends impl.IdCapture
  implicit object monad_Id extends impl.IdMonad
  implicit object par_Id extends impl.IdPar
  implicit object exceptions_Id extends impl.NoCaptureExceptions[Id] {
    implicit val E = monad_Id
  }
  implicit object delay_Id extends impl.BlockingDelay[Id] {
    override implicit val E = capture_Id
  }

  implicit def naturalTransformation_Id[E[_]](implicit
    E:Applicative[E]
  ) = new NaturalTransformation[Id,E] {
    override def apply[A](f: Id[A]): E[A] =
      E.pure(f)
  }

  /**
    * Add the liftS method to any effectful service that uses the LiftS
    * type-class to lift the service's effect system monad into another
    * effect system's monad
    */
  implicit class ServicePML[S[_[_]],F[_]](val self: S[F]) extends AnyVal {
    def liftService[G[_]](implicit
      X:CaptureTransform[F,G],
      liftService:LiftService[S]
    ) : S[G] = liftService(self)
  }

}
