package effectful

import scala.collection.generic.CanBuildFrom
import scala.concurrent.duration.FiniteDuration
import effectful.aspects._
import effectful.cats._
import effectful.impl.BlockingDelay


trait Exec[E[_]] extends
    Capture[E] with
    Monad[E] with
    Delay[E] with
    Par[E] with
    Exceptions[E]

object Exec {
  def apply[E[_]](implicit
    M: Monad[E],
    C: Capture[E],
    D: Delay[E],
    X: Exceptions[E],
    P: Par[E]
  ) : Exec[E] = new Exec[E] {
    def delay(duration: FiniteDuration) =
      D.delay(duration)

    def pure[A](a: A) =
      M.pure(a)
    def map[A, B](m: E[A])(f: (A) => B) =
      M.map(m)(f)
    def flatMap[A, B](m: E[A])(f: (A) => E[B]) =
      M.flatMap(m)(f)
    def widen[A, AA >: A](ea: E[A]) =
      M.widen(ea)

    def capture[A](a: => A) =
      C.capture(a)


    def attempt[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]]) =
      X.attempt(_try)(_catch)
    def attemptFinally[A, U](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]])(_finally: => E[U]) =
      X.attemptFinally(_try)(_catch)(_finally)
    def success[A](a: A) =
      X.success(a)
    def failure(t: Throwable) =
      X.failure(t)

    def par[A, B](ea: => E[A], eb: => E[B]) =
      P.par(ea,eb)
    def par[A, B, C](ea: => E[A], eb: => E[B], ec: => E[C]) =
      P.par(ea,eb,ec)
    def par[A, B, C, D](ea: => E[A], eb: => E[B], ec: => E[C], ed: => E[D]) =
      P.par(ea,eb,ec,ed)

    def parMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) =
      P.parMap(items)(f)
    def parFlatMap[M[AA] <: Seq[AA], A, B](items: M[A])(f: (A) => E[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) =
      P.parFlatMap(items)(f)

    def parMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => E[B])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) =
      P.parMapUnordered(items)(f)
    def parFlatMapUnordered[M[AA] <: Traversable[AA], A, B](items: M[A])(f: (A) => E[Traversable[B]])(implicit cbf: CanBuildFrom[Nothing, B, M[B]]) =
      P.parFlatMapUnordered(items)(f)
  }

  implicit def mkExec[E[_]](implicit
    M: Monad[E]
  ) : Exec[E] = {
    val C = Capture[E](M)
    Exec[E](
      M = M,
      C = C,
      D = BlockingDelay[E](C),
      X = Exceptions[E](M),
      P = Par[E](M)
    )
  }

}