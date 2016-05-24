import scala.collection.generic.CanBuildFrom

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
  implicit class MonadicOpsPML[E[_],A](val self: E[A]) extends AnyVal {
    def map[B](f: A => B)(implicit E:EffectSystem[E]) : E[B] =
      E.map(self)(f)
    def flatMap[B](f: A => E[B])(implicit E:EffectSystem[E]) : E[B]=
      E.flatMap(self)(f)
    def widen[AA >: A](implicit E:EffectSystem[E]) : E[AA] =
      E.widen(self)
  }

  /**
    * Add the sequence method to any Collection of a effect system's monad
    * that simply forwards the call to the implicit EffectSystem type-class
    * @param self collection of effects
    * @tparam F collection type
    * @tparam A type contained in collection
    */
  implicit class SequenceOpsPML[
    E[_],
    F[AA] <: Traversable[AA],
    A
  ](
    val self: F[E[A]]
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
      E: EffectSystem[E],
      cbf: CanBuildFrom[Nothing, A, F[A]]
    ) : E[F[A]] = {
      self.foldLeft(E(cbf())) { (eBuilder,ea) =>
        for {
          builder <- eBuilder
          a <- ea
        } yield builder += a
      }.map(_.result())
    }
  }

  /**
    * Implementation of EffectSystem type-class for the identity effect system
    * (which uses the identity monad)
    */
  implicit object EffectSystem_Id extends EffectSystem.Immediate[Id] with EffectSystem.NoExceptionCapture[Id] {
    override def map[A, B](
      m: Id[A]
    )(
      f: (A) => B
    ): Id[B] =
      f(m)
    override def flatMap[A, B](
      m: Id[A]
    )(
      f: (A) => Id[B]
    ): Id[B] =
      f(m)
    override def apply[A](a: => A): Id[A] =
      a
    override def widen[A, AA >: A](ea: Id[A]): Id[AA] =
      ea
    def foreach[A,U](ea: Id[A])(f: (A) => U) : Unit =
      f(ea)

    def flatSequence[F[_], A, B](
      ea: Id[A]
    )(
      f: (A) => F[Id[B]]
    )(implicit F: EffectSystem[F]) : F[Id[B]] =
      f(ea)
  }

  implicit def effectSystem_Free[Cmd[_]] = new EffectSystem[({ type E[AA] = Free[Cmd,AA] })#E] {
    def map[A, B](m: Free[Cmd, A])(f: (A) => B) =
      m.map(f)
    def flatMap[A, B](m: Free[Cmd, A])(f: (A) => Free[Cmd, B]) =
      m.flatMap(f)
    def apply[A](a: => A) = Free.Val(a)
    def widen[A, AA >: A](ea: Free[Cmd, A]) = ea.widen[AA]

    // todo: does this need to be encoded into Free?
    def Try[A](_try: => Free[Cmd, A])(_catch: PartialFunction[Throwable, Free[Cmd, A]]) = ???
    def TryFinally[A, U](_try: => Free[Cmd, A])(_catch: PartialFunction[Throwable, Free[Cmd, A]])(_finally: => Free[Cmd, U]) = ???
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

//  type FreeLambda[Cmd[_]] = ({ type F[A] = Free[Cmd,A]})#F

  implicit def liftE_Free[Cmd1[_],Cmd2[_]](implicit
    liftCmd:LiftCmd[Cmd1,Cmd2]
  ) = new LiftE[({ type F[AA] = Free[Cmd1,AA]})#F,({ type F[AA] = Free[Cmd2,AA]})#F] {
    override def apply[A](
      ea: => Free[Cmd1,A]
    )(implicit
      E: EffectSystem[({ type F[AA] = Free[Cmd1,AA]})#F],
      F: EffectSystem[({ type F[AA] = Free[Cmd2,AA]})#F]
    ): Free[Cmd2,A] =
      ea.liftCmd[Cmd2]
  }
}
