package effectful.aspects

trait Exceptions[E[_]] {
  /**
    * Replacement for standard try/catch blocks when using an effect
    * capture monad. Using this method ensures proper handling of
    * exceptions for monads that capture exception and for those
    * that don't.
    *
    * Note: the try/catch block does not properly catch exceptions from
    * monads that capture exceptions inside their monad E[A], such as Try,
    * Future or scalaz.Task. Using a try/catch block around a monad
    * such as Future will never execute the catch block.
    *
    * @param _try code block to catch exceptions from
    * @param _catch exception handler
    * @tparam A type of expression
    * @return an instance of E
    */
  def Try[A](
    _try: => E[A]
  )(
    _catch: PartialFunction[Throwable, E[A]]
  ) : E[A]

  /**
    * Replacement for standard try/catch/finally blocks when using an effect
    * capture monad. Using this method ensures proper handling of
    * exceptions for monads that capture exception and for those
    * that don't.
    *
    * Note: the try/catch block does not properly catch exceptions from
    * monads that capture exceptions inside their monad E[A], such as Try,
    * Future or scalaz.Task. Using a try/catch block around a monad
    * such as Future will never execute the catch block.
    *
    * @param _try code block to catch exceptions from
    * @param _catch exception handler
    * @tparam A type of expression
    * @return an instance of E
    */
  def TryFinally[A,U](
    _try: => E[A]
  )(
    _catch: PartialFunction[Throwable, E[A]]
  )(
    _finally: => E[U]
  ) : E[A]

  def success[A](a: A) : E[A]
  def failure(t: Throwable) : E[Nothing]
}
