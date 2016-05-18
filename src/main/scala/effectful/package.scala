import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

package object effectful {
  /**
    * Identity monad
    */
  type Id[A] = A

  // todo: this conflicts with std TraversableOnce.map/flatMap implicit class
  /**
    * Add the map/flatMap/widen methods to any effect system monad that
    * simply forward the call to the implicit EffectSystem type-class
    */
  implicit class MonadicOpsPML[E[_],A](val self: E[A]) extends AnyVal {
    def map[B](f: A => B)(implicit E:EffectSystem[E]) : E[B] =
      E.map(self, f)
    def flatMap[B](f: A => E[B])(implicit E:EffectSystem[E]) : E[B]=
      E.flatMap(self,f)
    def widen[AA >: A](implicit E:EffectSystem[E]) : E[AA] =
      E.widen(self)
  }

  /**
    * Add the sequence method to any Collection of a effect system's monad
    * that simply forwards the call to the implicit EffectSystem type-class
    */
  implicit class SequenceOpsPML[E[_],F[AA] <: Traversable[AA],A](val self: F[E[A]]) extends AnyVal {
    def sequence(implicit
      E:EffectSystem[E]
    ) : E[F[A]] = E.sequence(self)
  }

  /**
    * Implementation of EffectSystem type-class for the identity effect system
    * (which uses the identity monad)
    */
  implicit object EffectSystem_Id extends EffectSystem[Id] {
    override def map[A, B](m: Id[A], f: (A) => B): Id[B] =
      f(m)
    override def flatMap[A, B](m: Id[A], f: (A) => Id[B]): Id[B] =
      f(m)
    override def apply[A](a: => A): Id[A] =
      a
    override def sequence[F[AA] <: Traversable[AA], A](fea: F[Id[A]]): Id[F[A]] =
      fea
    override def delay(duration: FiniteDuration): Id[Unit] =
      Thread.sleep(duration.toMillis)
    override def widen[A, AA >: A](ea: Id[A]): Id[AA] = ea
    override def Try[A](f: => Id[A])(_catch: PartialFunction[Throwable, Id[A]]): Id[A] =
      try { f } catch _catch
  }

  /**
    * Automatically create a LiftE type-class instance that can
    * lift from identity effect system into any other effect system
    */
  implicit def liftE_Id[F[_]] : LiftE[Id,F] = new LiftE[Id,F] {
    override def apply[A](
      ea: => Id[A]
    )(implicit
      E:EffectSystem[Id],
      F:EffectSystem[F]
    ) : F[A] = F(ea)
  }

  /**
    * Add the liftE method to any effect system's monad that uses the LiftE
    * type-class to lift the monad into another effect system's monad
    */
  implicit class EffectSystemPml[E[_],A](val self: E[A]) extends AnyVal {
    def liftE[F[_]](implicit
      E:EffectSystem[E],
      F:EffectSystem[F],
      liftE:LiftE[E,F]
    ) : F[A] = liftE(self)
  }

  /**
    * Add the liftS method to any effectful service that uses the LiftS
    * type-class to lift the service's effect system monad into another
    * effect system's monad
    */
  implicit class ServicePML[S[_[_]],E[_]](val self: S[E]) extends AnyVal {
    def liftS[F[_]](implicit
      E:EffectSystem[E],
      F:EffectSystem[F],
      liftE:LiftE[E,F],
      liftS:LiftS[S]
    ) : S[F] = liftS(self)
  }


}
