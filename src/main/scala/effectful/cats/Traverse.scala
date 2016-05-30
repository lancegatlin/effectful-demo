package effectful.cats

trait Traverse[T[_]] {
  def foreach[A,U](ea: T[A])(f: A => U) : Unit

  def flatSequence[M[_],A,B](
    ta: T[A]
  )(
    f: A => M[T[B]]
  )(implicit M:Monad[M]) : M[T[B]]
}

object Traverse {
  object ops {
    implicit class TraverseOpsPML[T[_],A](val self: T[A]) extends AnyVal {
      def foreach[U](f: A => U)(implicit T:Traverse[T]) : Unit =
        T.foreach(self)(f)
    }
  }
}