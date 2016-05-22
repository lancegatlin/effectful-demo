package effectful

import scala.language.higherKinds

/**
  * An effect system derived from nesting the monad G inside F
  *
  * Note: takes the place of monad transformers
  * Note: G must be an immediate effect system
  *
  * @tparam F the outer effect system monad
  * @tparam G the inner effect system monad, must compute results immediatly
  */
trait Nested[F[_],G[_]] extends EffectSystem[({ type FG[A] = F[G[A]]})#FG] {
  implicit val F:EffectSystem[F]
  implicit val G:EffectSystem.Immediate[G]

  def map[A, B](m: F[G[A]])(f: (A) => B) =
    F.map(m)(ga => G.map(ga)(f))
  def flatMap[A, B](m: F[G[A]])(f: (A) => F[G[B]]) =
    F.flatMap(m)(ga => G.flatSequence(ga)(f))
  def Try[A](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]]) =
    // todo: think about how or if this should interact with inner effect systems G.Try
    F.Try(_try)(_catch)
  def TryFinally[A,U](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]])(_finally: => F[G[U]]) =
    F.TryFinally(_try)(_catch)(_finally)
  def widen[A, AA >: A](fga: F[G[A]]) =
  // todo: better way to do this that avoids runtime?
    fga.map(_.widen)
  def apply[A](a: => A) =
    F(G(a))
}

object Nested {
  def apply[F[_],G[_]](implicit
    F:EffectSystem[F],
    G:EffectSystem.Immediate[G]
  ) : Nested[F,G] = {
    val _F = F
    val _G = G
    new Nested[F,G] {
      implicit val F = _F
      implicit val G = _G
    }
  }
}