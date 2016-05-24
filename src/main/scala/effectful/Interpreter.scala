package effectful

trait Interpreter[Cmd[_],E[_]] {
  def apply[A](cmd: Cmd[A]) : E[A]
}