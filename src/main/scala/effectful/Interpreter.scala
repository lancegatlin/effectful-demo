package effectful

trait Interpreter[Cmd[_],E[_]] {
  implicit val E: EffectSystem[E]
  def apply[A](cmd: Cmd[A]) : E[A]
}