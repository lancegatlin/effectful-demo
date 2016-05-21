package effectful

import scala.language.higherKinds

trait Immediate[E[_]] extends EffectSystem[E] {
  def foreach[A,U](ea: E[A])(f: A => U) : Unit

  def flatSequence[F[_],A,B](ea: E[A])(f: A => F[E[B]])(implicit F:EffectSystem[F]) : F[E[B]]

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
}
