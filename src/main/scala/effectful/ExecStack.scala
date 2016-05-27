package effectful
import scala.concurrent.duration.FiniteDuration

/**
  * An exec monad derived from nesting the exec monad G inside
  * the exec monad F
  *
  * Note: takes the place of monad transformers
  * Note: inner exec monad must be immediate
  *
  * @tparam F the outer exec monad
  * @tparam G the inner exec monad, must compute results immediately
  */
trait ExecStack[F[_],G[_]] extends 
  Exec[({ type FG[A] = F[G[A]]})#FG] with
  impl.StdPar[({ type FG[A] = F[G[A]]})#FG]
{
  type E[A] = F[G[A]]
  implicit val E:Exec[E] = this
  
  implicit val F:Exec[F]
  implicit val G:Exec.ImmediateNoCaptureExceptions[G]

  def map[A, B](m: E[A])(f: A => B) =
    F.map(m)(ga => G.map(ga)(f))
  def flatMap[A, B](m: E[A])(f: (A) => E[B]) =
    F.flatMap(m)(ga => G.flatSequence(ga)(f))
  def widen[A, AA >: A](fga: E[A]) =
  // todo: better way to do this that avoids runtime?
    F.map(fga)(_.widen)
  def apply[A](a: => A) =
    F(G(a))

  // todo: maybe way to unpack exception from inner here?
  def Try[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]]) =
    F.Try(_try)(_catch)
  def TryFinally[A,U](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]])(_finally: => E[U]) =
    F.TryFinally(_try)(_catch)(_finally)
  override def failure(t: Throwable): E[Nothing] =
    F.failure(t).asInstanceOf
  override def success[A](a: A): E[A] =
    F.success(G(a))

  override def delay(duration: FiniteDuration): E[Unit] =
    F.map(F.delay(duration))(_ => G(()))
}

object ExecStack {
  def apply[F[_],G[_]](implicit
    F:Exec[F],
    G:Exec.ImmediateNoCaptureExceptions[G]
  ) : ExecStack[F,G] = {
    val _F = F
    val _G = G
    new ExecStack[F,G] {
      implicit val F = _F
      implicit val G = _G
    }
  }
}