package effectful

import effectful.cats._
import effectful.aspects._

object CompositeExec {
  def apply[F[_],G[_]](implicit
    captureF: Capture[F],
    monadF: Monad[F],
    delayF: Delay[F],
    exceptionsF: Exceptions[F],
    parF: Par[F],
    monadG: Monad[G],
    flatSequenceFG: FlatSequence[F,G]
  ) : Exec[({type FG[A] = F[G[A]]})#FG] = {
    type FG[A] = F[G[A]]

    new
      CompositeCapture[F,G] with
      CompositeMonad[F,G] with
      CompositeExceptions[F,G] with
      CompositePar[F,G] with
      CompositeDelay[F,G] {
      override val F: Monad[F] = monadF
      override val G: Monad[G] = monadG

      val C: Capture[F] = captureF
      val X: Exceptions[F] = exceptionsF
      val P: Par[F] = parF
      val D: Delay[F] = delayF

      def maybeCapture[A](a: => A) =
        monadG.pure(a)

      def flatSequence[A, B](ga: G[A])(f: (A) => F[G[B]]) =
        flatSequenceFG.flatSequence(ga)(f)
    }
  }
}
