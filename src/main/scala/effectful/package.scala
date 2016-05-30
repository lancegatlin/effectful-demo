
import scala.collection.generic.CanBuildFrom
import effectful.cats.{Capture, Monad}
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

  /**
    * Implementation of EffectSystem type-class for the identity effect system
    * (which uses the identity monad)
    */
  implicit object exec_Id extends
    impl.IdCapture with
    impl.IdMonad with
    impl.IdTraverse with
    impl.IdPar with
    impl.NoCaptureExceptions[Id] with
    impl.BlockingDelay[Id] {
    implicit override val E: Monad[Id] with Capture[Id] = this
  }

  /**
    * Automatically create a LiftE type-class instance that can
    * lift from identity effect system into any other effect system
    */
  implicit def liftCapture_Id[F[_]](implicit
    F:Capture[F]
  ) : LiftCapture[Id,F] = new LiftCapture[Id,F] {
    override def apply[A](
      ea: => Id[A]
    ) : F[A] = F.capture(ea)
  }

  /**
    * Add the liftCapture method to any effect system's monad that uses the LiftE
    * type-class to lift the monad into another effect system's monad
    */
  implicit class EffectSystemPml[E[_],A](val self: E[A]) extends AnyVal {
    def liftCapture[F[_]](implicit
      liftCapture:LiftCapture[E,F]
    ) : F[A] = liftCapture(self)
  }

  /**
    * Add the liftS method to any effectful service that uses the LiftS
    * type-class to lift the service's effect system monad into another
    * effect system's monad
    */
  implicit class ServicePML[S[_[_]],E[_]](val self: S[E]) extends AnyVal {
    def liftService[F[_]](implicit
      liftCapture:LiftCapture[E,F],
      liftService:LiftService[S]
    ) : S[F] = liftService(self)
  }

//  implicit def captureFromMonad[E[_]](implicit E:Monad[E]) : Capture[E] =
//    new Capture[E] {
//      def capture[A](a: => A) = E.pure(a)
//    }
}
