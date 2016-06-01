package effectful.cats

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
  implicit def apply[F[_],G[_]](implicit
    C:Capture[F],
    G:Applicative[G]
  ) : Capture[({ type FG[A] = F[G[A]]})#FG] =
    CompositeCapture[F,G]
}