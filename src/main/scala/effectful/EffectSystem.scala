package effectful

import scala.concurrent.duration.FiniteDuration
import scala.language.higherKinds

/**
  * A type-class for an effect system's monad that can be used to
  * capture and isolate the effects of a computation
  *
  * @tparam E monad type
  */
trait EffectSystem[E[_]] {
  def map[A,B](m: E[A], f: A => B) : E[B]
  def flatMap[A,B](m: E[A], f: A => E[B]) : E[B]

  /**
    * Create an instance of E that can capture the effects of
    * a computation of a value
    *
    * Note1: computation may fail with an exception
    * Note2: parameter must be lazy to allow for capture of effects
    *
    * @param a computation
    * @tparam A type of result of computation
    * @return an instance of E that can capture the effects of the computation
    */
  def apply[A](a: => A) : E[A]

  def sequence[F[AA] <: Traversable[AA],A](fea: F[E[A]]) : E[F[A]]

  def widen[A,AA >: A](ea: E[A]) : E[AA]

  /**
    * Replacement for standard try/catch blocks. Using this method ensures
    * proper handling of exceptions for both effect systems that capture
    * exception and those that don't.
    *
    * Note: the try/catch block does not properly catch exceptions from
    * effect systems that capture exceptions inside their monad E[A],
    * such as Try, Future or scalaz.Task. Using a try/catch block around
    * an effect system such as Future will execute the catch block.
    *
    * @param f code block to catch exceptions from
    * @param _catch exception handler
    * @tparam A type of expression
    * @return an instance of E
    */
  def Try[A](f: => E[A])(_catch: PartialFunction[Throwable, E[A]]) : E[A]

  // def success(a: A) : E[A] ?
  // def failure(t: Throwable) : E[A] ?
  // todo: move to effect service (DelayService)
  def delay(duration: FiniteDuration) : E[Unit]
}