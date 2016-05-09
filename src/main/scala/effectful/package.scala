import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

package object effectful {
  type Id[+A] = A

  implicit class MonadicOps[E[_],A](val self: E[A]) extends AnyVal {
    def map[B](f: A => B)(implicit E:EffectSystem[E]) : E[B] =
      E.map(self, f)
    def flatMap[B](f: A => E[B])(implicit E:EffectSystem[E]) : E[B]=
      E.flatMap(self,f)
  }

  implicit class SequenceOps[E[_],F[AA] <: Traversable[AA],A](val self: F[E[A]]) extends AnyVal {
    def sequence(implicit
      E:EffectSystem[E]
    ) : E[F[A]] = E.sequence(self)
  }

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
  }

  implicit def liftE_Id[F[_]] : LiftE[Id,F] = new LiftE[Id,F] {
    override def apply[A](
      ea: => Id[A]
    )(implicit
      E:EffectSystem[Id],
      F:EffectSystem[F]
    ) : F[A] = F(ea)
  }

  implicit class EffectSystemPml[E[_],A](val self: E[A]) extends AnyVal {
    def liftE[F[_]](implicit
      E:EffectSystem[E],
      F:EffectSystem[F],
      liftE:LiftE[E,F]
    ) : F[A] = liftE(self)
  }

  implicit class ServicePML[S[_[_]],E[_]](val self: S[E]) extends AnyVal {
    def liftS[F[_]](implicit
      E:EffectSystem[E],
      F:EffectSystem[F],
      liftE:LiftE[E,F],
      liftS:LiftS[S]
    ) : S[F] = liftS(self)
  }


}
