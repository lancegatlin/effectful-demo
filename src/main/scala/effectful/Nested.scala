package effectful

import scala.language.higherKinds

trait Nested[F[_],G[_]] extends EffectSystem[({ type FG[A] = F[G[A]]})#FG] {

  implicit val F:EffectSystem[F]
  implicit val G:EffectSystem.Immediate[G]

  def map[A, B](m: F[G[A]])(f: (A) => B) =
    F.map(m)(ea => G.map(ea)(f))
  def flatMap[A, B](m: F[G[A]])(f: (A) => F[G[B]]) =
    F.flatMap(m)(ea => G.flatSequence(ea)(f))
  def Try[A](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]]) =
    F.Try(_try)(_catch)
  def TryFinally[A,U](_try: => F[G[A]])(_catch: PartialFunction[Throwable, F[G[A]]])(_finally: => F[G[U]]) =
    F.TryFinally(_try)(_catch)(_finally)
  def widen[A, AA >: A](ea: F[G[A]]) =
    // todo: can this just be cast to avoid cost of map?
    ea.map(_.widen)
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