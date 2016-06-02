package effectful.aspects

import effectful.cats.Applicative

trait CompositeExceptions[F[_],G[_]] extends Exceptions[({ type FG[A] = F[G[A]]})#FG] {
  val X:Exceptions[F]
  val G:Applicative[G]

  // todo: maybe way to unpack exception from inner here?
  override def attempt[A](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]]) =
    X.attempt(_try)(_catch)
  override def attemptFinally[A,U](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]])(_finally: => F[G[U]]) =
    X.attemptFinally(_try)(_catch)(_finally)
  override def failure(t: Throwable): F[G[Nothing]] =
    X.failure(t).asInstanceOf
  override def success[A](a: A): F[G[A]] =
    X.success(G.pure(a))
}

object CompositeExceptions {
  implicit def apply[F[_],G[_]](implicit
    X:Exceptions[F],
    G:Applicative[G]
  ) = {
    val _X = X
    val _G = G
    new CompositeExceptions[F,G] {
      override val X = _X
      override val G = _G
    }
  }
}