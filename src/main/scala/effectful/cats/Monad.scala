package effectful.cats

trait Monad[E[_]] extends Applicative[E] {
  def map[A,B](m: E[A])(f: A => B) : E[B]
  def flatMap[A,B](m: E[A])(f: A => E[B]) : E[B]

  /**
    * An effect capture monad should be covariant, however, to preserve compatibility
    * with scalaz, M as invariant in its parameter. This method restores covariance
    * when needed. It should ideally be implemented in a way that has no or minimal
    * runtime impact.
    *
    * @param ea instance of effect system's monad
    * @tparam A type contained in monad
    * @tparam AA some super type of A
    * @return ea cast to E[AA]
    */
  def widen[A,AA >: A](ea: E[A]) : E[AA]
}

object Monad {
  object ops {
    /**
      * Add the map/flatMap/widen methods to any effect system monad that
      * simply forward the call to the implicit EffectSystem type-class
      */
    // todo: this conflicts with std TraversableOnce.map/flatMap implicit class
    // todo: how does scalaz handle this?
    implicit class MonadicOpsPML_XjJsYyBXXE[E[_],A](val self: E[A]) extends AnyVal {
      def map[B](f: A => B)(implicit E:Monad[E]) : E[B] =
        E.map(self)(f)
      def flatMap[B](f: A => E[B])(implicit E:Monad[E]) : E[B]=
        E.flatMap(self)(f)
      def widen[AA >: A](implicit E:Monad[E]) : E[AA] =
        E.widen(self)
    }
    implicit class EverythingPML_XjJsYyBXXE[A](val self: A) extends AnyVal {
      def pure[E[_]](implicit E:Monad[E]) : E[A] =
        E.pure(self)
    }
    implicit class MonadPML[E[_]](val self: Monad[E]) extends AnyVal {
      def apply[A](a: A) : E[A] =
        self.pure(a)
    }
  }
}
