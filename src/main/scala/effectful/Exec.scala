package effectful

import effectful.cats.{Capture, Monad, Traverse}
import effectful.aspects._

/**
  * A type-class for a monad (or monad stack) that can be used
  * to capture and isolate certain effects of a computation.
  *
  * Exactly what kinds of effects are captured (such as logging or IO)
  * and how the computation itself is eventually computed depends on the
  * monad. Some monads may lazily execute computations, some may distribute
  * computations to be executed asynchronously and others may simply capture the
  * program for later execution or serialization.
  * For monads that do execute code directly, exceptions may be captured as a
  * value in the monad or the exception may be thrown.
  *
  * Some common exec monads: Try, Future, scalaz.Task,
  * scalaz.IO, scalaz.Writer, free monad, identity monad (type Id[A] = A)
  *
  * @tparam E monad type
  */
trait Exec[E[_]] extends
  Capture[E] with
  Monad[E] with
  Delay[E] with
  Exceptions[E] with
  Par[E]

object Exec {
  /**
  * A monad that immediately (i.e. eagerly & non-asynchronously) executes
  * its computations. Once computed, the monad's value(s) can be traversed without
  * side effects.
  *
  * @tparam E monad type
  */
  trait Immediate[E[_]] extends
    Exec[E] with
    Traverse[E] with
    impl.BlockingDelay[E] {
    implicit val E:Capture[E]
  }

/**
  * A monad that is immediate and does not capture exceptions.
  *
  * @tparam E monad type
  */
  trait ImmediateNoCaptureExceptions[E[_]] extends
    Immediate[E] with
    impl.NoCaptureExceptions[E] {
    implicit override val E:Monad[E] with Capture[E]
  }
}
