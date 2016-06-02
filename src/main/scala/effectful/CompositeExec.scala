package effectful

import effectful.cats._
import effectful.aspects._

object CompositeExec {
  def apply[F[_],G[_]](implicit
    execF: Exec[F],
    captureG: Capture[G],
    monadG: Monad[G],
    flatSequenceFG: FlatSequence[F,G]
  ) : Exec[({type FG[A] = F[G[A]]})#FG] = {
    type FG[A] = F[G[A]]

    new Exec[FG] with
      CompositeCapture[F,G] with
      CompositeMonad[F,G] with
      CompositeExceptions[F,G] with
      CompositePar[F,G] with
      CompositeDelay[F,G] {
      override val F: Monad[F] = execF
      override val G: Monad[G] = monadG

      val C: Capture[F] = execF
      val X: Exceptions[F] = execF
      val P: Par[F] = execF
      val D: Delay[F] = execF

      def maybeCapture[A](a: => A) =
        captureG.capture(a)

      def flatSequence[A, B](ga: G[A])(f: (A) => F[G[B]]) =
        flatSequenceFG.flatSequence(ga)(f)
    }
  }
}
