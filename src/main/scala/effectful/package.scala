import scala.collection.generic.CanBuildFrom
import effectful.cats.{Monad, Traverse}

package object effectful {
  /**
    * Identity monad
    */
  type Id[A] = A

  // todo: figure out how this sugar is declared in emm
//  type |:[F[_],G[_]] = F[G[_]]
//  val |: = Nested

  // todo: this conflicts with std TraversableOnce.map/flatMap implicit class
  // todo: how does scalaz handle this?
  /**
    * Add the map/flatMap/widen methods to any effect system monad that
    * simply forward the call to the implicit EffectSystem type-class
    */
  implicit class MonadicOpsPML[M[_],A](val self: M[A]) extends AnyVal {
    def map[B](f: A => B)(implicit M:Monad[M]) : M[B] =
      M.map(self)(f)
    def flatMap[B](f: A => M[B])(implicit M:Monad[M]) : M[B]=
      M.flatMap(self)(f)
    def widen[AA >: A](implicit M:Monad[M]) : M[AA] =
      M.widen(self)
  }

  implicit class TraverseOpsPML[T[_],A](val self: T[A]) extends AnyVal {
    def foreach[U](f: A => U)(implicit T:Traverse[T]) : Unit =
      T.foreach(self)(f)
  }
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
  implicit object Exec_Id extends
    Exec.ImmediateNoCaptureExceptions[Id] with
    impl.IdMonad with
    impl.IdTraverse with
    impl.IdPar {
      override implicit val E = this
    }

  /**
    * Automatically create a LiftE type-class instance that can
    * lift from identity effect system into any other effect system
    */
  implicit def liftE_Id[F[_]] : LiftExec[Id,F] = new LiftExec[Id,F] {
    override def apply[A](
      ea: => Id[A]
    )(implicit
      E: Exec[Id],
      F: Exec[F]
    ) : F[A] = F(ea)
  }

  /**
    * Add the liftExec method to any effect system's monad that uses the LiftE
    * type-class to lift the monad into another effect system's monad
    */
  implicit class EffectSystemPml[E[_],A](val self: E[A]) extends AnyVal {
    def liftExec[F[_]](implicit
      E: Exec[E],
      F: Exec[F],
      liftExec:LiftExec[E,F]
    ) : F[A] = liftExec(self)
  }

  /**
    * Add the liftS method to any effectful service that uses the LiftS
    * type-class to lift the service's effect system monad into another
    * effect system's monad
    */
  implicit class ServicePML[S[_[_]],E[_]](val self: S[E]) extends AnyVal {
    def liftService[F[_]](implicit
      E: Exec[E],
      F: Exec[F],
      liftExec:LiftExec[E,F],
      liftS:LiftService[S]
    ) : S[F] = liftS(self)
  }

}
