package effectful.free

import effectful.Exec

trait Interpreter[Cmd[_],E[_]] {
  implicit val E: Exec[E]
  def apply[A](cmd: Cmd[A]) : E[A]
}