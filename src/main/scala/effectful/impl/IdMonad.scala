package effectful.impl

import effectful._
import effectful.cats.Monad

trait IdMonad extends Monad[Id] {
  override def map[A, B](m: Id[A])(f: (A) => B): Id[B] =
    f(m)

  override def flatMap[A, B](m: Id[A])(f: (A) => Id[B]): Id[B] =
    f(m)

  override def widen[A, AA >: A](ea: Id[A]): Id[AA] =
    ea

  override def apply[A](a: => A): Id[A] =
    a
}
