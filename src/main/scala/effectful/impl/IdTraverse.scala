package effectful.impl

import effectful._
import effectful.cats.{Monad, Traverse}

trait IdTraverse extends Traverse[Id] {
  override def foreach[A, U](ta: Id[A])(f: (A) => U): Unit =
    f(ta)

  override def flatSequence[M[_], A, B](ta: Id[A])(f: (A) => M[Id[B]])(implicit M: Monad[M]): M[Id[B]] =
    f(ta)
}
