package effectful

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

/**
  * A type-class for an effect system's monad that can be used to
  * capture and isolate the effects of a computation.
  *
  * Exactly what kinds of effects are captured (such as logging or IO)
  * and how the computation itself is eventually computed depends on the
  * effect system's monad. Some monads may lazily execute computations,
  * some may distribute code to be executed asynchronously and others
  * may simply capture the program for later execution or serialization.
  * For monads that execute code, some monads may capture exceptions
  * explicitly and others may simply throw them in the callers thread.
  *
  * Some common effect system monads: Try, Future, scalaz.Task,
  * scalaz.IO, scalaz.Writer, free monad, identity monad (type Id[A] = A)
  *
  * @tparam E monad type
  */
trait EffectSystem[E[_]] {
  def map[A,B](m: E[A], f: A => B) : E[B]
  def flatMap[A,B](m: E[A], f: A => E[B]) : E[B]

  /**
    * Create an instance of E that may capture the effects of
    * a computation of a value
    *
    * Note1: computation may fail with an exception and may
    * or may not be captured by the effect system monad
    * Note2: parameter must be lazy to allow for capture of effects
    *
    * @param a computation
    * @tparam A type of result of computation
    * @return an instance of E that can capture the effects of the computation
    */
  def apply[A](a: => A) : E[A]

  /**
    * Effect system's monad should be covariant, however, to preserve compatibility
    * with scalaz, EffectSystem declares E as invariant. This method restores covariance
    * when needed. It should ideally be implemented in a way that has no or minimal runtime
    * impact.
    *
    * @param ea instance of effect system's monad
    * @tparam A type contained in monad
    * @tparam AA some super type of A
    * @return ea cast to E[AA]
    */
  def widen[A,AA >: A](ea: E[A]) : E[AA]

  /**
    * Sequence a collection of effects into an effect of the collection
    *
    * Note: this method is unnecessary if using scalaz and is here for
    * compatibility if not using scalaz
    *
    * @param fea collection of effects
    * @tparam F collection type
    * @tparam A type contained in collection
    * @return
    */
  def sequence[F[AA] <: Traversable[AA],A](fea: F[E[A]])(implicit cbf: CanBuildFrom[Nothing, A, F[A]]) : E[F[A]] = {
    implicit val E = this
    val baseBuilder = cbf()
    baseBuilder.sizeHint(fea)
    fea.foldLeft(apply(baseBuilder)) { (fBuilder,ea) =>
      for {
        builder <- fBuilder
        a <- ea
      } yield builder += a
    }.map(_.result())
  }

  /**
    * Replacement for standard try/catch blocks when using an effect
    * system's monad. Using this method ensures proper handling of
    * exceptions for monads that capture exception and for those
    * that don't.
    *
    * Note: the try/catch block does not properly catch exceptions from
    * effect systems that capture exceptions inside their monad E[A],
    * such as Try, Future or scalaz.Task. Using a try/catch block around
    * an effect system such as Future will never execute the catch block.
    *
    * @param _try code block to catch exceptions from
    * @param _catch exception handler
    * @tparam A type of expression
    * @return an instance of E
    */
  def Try[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]]) : E[A]

  /**
    * Replacement for standard try/catch blocks when using an effect
    * system's monad. Using this method ensures proper handling of
    * exceptions for monads that capture exception and for those
    * that don't.
    *
    * Note: the try/catch block does not properly catch exceptions from
    * effect systems that capture exceptions inside their monad E[A],
    * such as Try, Future or scalaz.Task. Using a try/catch block around
    * an effect system such as Future will never execute the catch block.
    *
    * @param _try code block to catch exceptions from
    * @param _catch exception handler
    * @tparam A type of expression
    * @return an instance of E
    */
  def Try[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]])(_finally: => E[Unit]) : E[A]

  // todo: def success(a: A) : E[A] ?
  // todo: def failure(t: Throwable) : E[A] ?
}