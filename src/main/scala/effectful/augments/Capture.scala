package effectful.augments

import cats._

trait Capture[E[_]] {
  /**
    * Create an instance of E that may capture some of the
    * effects of a computation or might change how the computation
    * is executed (e.g. asynchronous or lazy)
    *
    * Note1: exceptions may or may not be captured
    * Note2: parameter is lazy to allow for widest possible capture
    * of effects
    *
    * @param a computation
    * @tparam A type of result of computation
    * @return an instance of E for the computation
    */
  def capture[A](a: => A) : E[A]
}

object Capture {
  // todo: how to make this implicit without ambiguous?
  def fromApplicative[E[_]](implicit
    E:Applicative[E]
  ) : Capture[E] = new Capture[E] {
    override def capture[A](a: => A): E[A] =
      E.pure(a)
  }

  implicit def apply[F[_],G[_]](implicit
    C:Capture[F],
    G:Capture[G]
  ) : Capture[({ type FG[A] = F[G[A]]})#FG] =
    CompositeCapture[F,G]
}