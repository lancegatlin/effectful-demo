package effectful.impl

import effectful.augments.Exceptions
import effectful.cats.Monad

/**
  * An instance of Exceptions for monads that don't capture exceptions
  * in the monad
  */
trait NoCaptureExceptions[E[_]] extends Exceptions[E] {
  implicit val E:Monad[E]

  override def attempt[A](
   _try: =>E[A]
  )(
   _catch: PartialFunction[Throwable, E[A]]
  ): E[A] =
    try { _try } catch _catch

  override def attemptFinally[A,U](
    _try: => E[A]
  )(
    _catch: PartialFunction[Throwable, E[A]]
  )(
    _finally: => E[U]
  ): E[A] =
    try { _try } catch _catch finally _finally

  override def success[A](a: A): E[A] =
    E.pure(a)

  override def failure(t: Throwable): E[Nothing] =
    throw t
}
