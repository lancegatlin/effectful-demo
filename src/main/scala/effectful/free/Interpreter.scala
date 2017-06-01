package effectful.free

import cats._
import effectful.augments._

trait Interpreter[Cmd[_],E[_]] {
  val C:Capture[E]
  val M:Monad[E]
  val D:Delay[E]
  val P:Par[E]
  val X:Exceptions[E]

  def apply[A](cmd: Cmd[A]) : E[A]
}