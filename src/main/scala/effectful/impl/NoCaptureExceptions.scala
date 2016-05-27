package effectful.impl

import effectful.Exec
import effectful.aspects.Exceptions

/**
  * An effect system that doesn't capture exceptions in the effect system's monad
  */
trait NoCaptureExceptions[E[_]] extends Exceptions[E] {
  implicit val E:Exec[E]

  override def Try[A](
   _try: =>E[A]
  )(
   _catch: PartialFunction[Throwable, E[A]]
  ): E[A] =
    try { _try } catch _catch

  override def TryFinally[A,U](
    _try: => E[A]
  )(
    _catch: PartialFunction[Throwable, E[A]]
  )(
    _finally: => E[U]
  ): E[A] =
    try { _try } catch _catch finally _finally

  override def success[A](a: A): E[A] =
    E(a)

  override def failure(t: Throwable): E[Nothing] =
    throw t
}
