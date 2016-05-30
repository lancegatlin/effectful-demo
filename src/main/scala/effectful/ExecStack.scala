package effectful

/**
  * An exec monad derived from nesting the exec monad G inside
  * the exec monad F
  *
  * Note: takes the place of monad transformers
  * Note: inner exec monad must be immediate
  *
  * @tparam F the outer exec monad
  * @tparam G the inner exec monad, must compute results immediately
  */
trait ExecStack[F[_],G[_]] extends 
  Exec[({ type FG[A] = F[G[A]]})#FG]
{
  type E[A] = F[G[A]]
  implicit val E:Exec[E] = this

  implicit val F:Exec[F]
  implicit val G:Exec.ImmediateNoCaptureExceptions[G]
}

object ExecStack {
  def apply[F[_],G[_]](implicit
    F:Exec[F],
    G:Exec.ImmediateNoCaptureExceptions[G]
  ) : ExecStack[F,G] = {
    val _F = F
    val _G = G
    new impl.ExecStackImpl[F,G] {
      implicit val F = _F
      implicit val G = _G
    }
  }
}