package effectful.free

trait LiftCmd[Cmd1[_],Cmd2[_]] {
  def apply[AA](
    cmd: Cmd1[AA]
  ) : Cmd2[AA]
}
