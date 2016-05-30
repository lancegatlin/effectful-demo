package effectful.impl

import effectful._
import effectful.cats.Monad

import scala.concurrent.duration.FiniteDuration

trait ExecStackImpl[F[_],G[_]] extends
  ExecStack[F,G] with
  impl.StdPar[({ type FG[A] = F[G[A]]})#FG]
{
  import Monad.ops._

  // todo: it should be possible for F to be Future and G a Future
  // todo: the only thing really required is flatSequence
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
  def attempt[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]]) =
    F.attempt(_try)(_catch)
  def attemptFinally[A,U](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]])(_finally: => E[U]) =
    F.attemptFinally(_try)(_catch)(_finally)
  override def failure(t: Throwable): E[Nothing] =
    F.failure(t).asInstanceOf
  override def success[A](a: A): E[A] =
    F.success(G(a))

  override def delay(duration: FiniteDuration): E[Unit] =
    F.map(F.delay(duration))(_ => G(()))
}
