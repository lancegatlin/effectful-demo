package effectful.cats

trait Traverse[T[_]] {
  def foreach[A,U](ea: T[A])(f: A => U) : Unit

  def flatSequence[M[_],A,B](
    ta: T[A]
  )(
    f: A => M[T[B]]
  )(implicit M:Monad[M]) : M[T[B]]
}