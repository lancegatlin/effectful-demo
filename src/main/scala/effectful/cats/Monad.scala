package effectful.cats

trait Monad[M[_]] {
  def map[A,B](m: M[A])(f: A => B) : M[B]
  def flatMap[A,B](m: M[A])(f: A => M[B]) : M[B]

  /**
    * Lift an already computed value into the monad
    * @param a computed value
    * @return an instance of M
    */
  def pure[A](a: A) : M[A]

  /**
    * An effect capture monad should be covariant, however, to preserve compatibility
    * with scalaz, M as invariant in its parameter. This method restores covariance
    * when needed. It should ideally be implemented in a way that has no or minimal
    * runtime impact.
    *
    * @param ea instance of effect system's monad
    * @tparam A type contained in monad
    * @tparam AA some super type of A
    * @return ea cast to M[AA]
    */
  def widen[A,AA >: A](ea: M[A]) : M[AA]
}

object Monad {
  object ops {
    /**
      * Add the map/flatMap/widen methods to any effect system monad that
      * simply forward the call to the implicit EffectSystem type-class
      */
    // todo: this conflicts with std TraversableOnce.map/flatMap implicit class
    // todo: how does scalaz handle this?
    implicit class MonadicOpsPML_XjJsYyBXXE[M[_],A](val self: M[A]) extends AnyVal {
      def map[B](f: A => B)(implicit M:Monad[M]) : M[B] =
        M.map(self)(f)
      def flatMap[B](f: A => M[B])(implicit M:Monad[M]) : M[B]=
        M.flatMap(self)(f)
      def widen[AA >: A](implicit M:Monad[M]) : M[AA] =
        M.widen(self)
    }
    implicit class EverythingPML_XjJsYyBXXE[A](val self: A) extends AnyVal {
      def pure[M[_]](implicit M:Monad[M]) : M[A] =
        M.pure(self)
    }
    implicit class MonadPML[M[_]](val self: Monad[M]) extends AnyVal {
      def apply[A](a: A) : M[A] =
        self.pure(a)
    }
  }
}
