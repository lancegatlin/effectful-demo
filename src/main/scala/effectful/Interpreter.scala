package effectful

trait Interpreter[Cmd[_],E[_]] {
  def apply[A](cmd: Cmd[A])(implicit E:EffectSystem[E]) : E[A]
}