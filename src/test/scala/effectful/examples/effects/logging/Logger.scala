package effectful.examples.effects.logging

trait Logger[E[_]] {
  def trace(message: => String) : E[Unit]
  def trace(message: => String, cause: Throwable) : E[Unit]
  def debug(message: => String) : E[Unit]
  def debug(message: => String, cause: Throwable) : E[Unit]
  def info(message: => String) : E[Unit]
  def info(message: => String, cause: Throwable) : E[Unit]
  def warn(message: => String) : E[Unit]
  def warn(message: => String, cause: Throwable) : E[Unit]
  def error(message: => String) : E[Unit]
  def error(message: => String, cause: Throwable) : E[Unit]
}