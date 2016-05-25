package effectful.examples.effects.logging

trait LoggerFactory[E[_]] {
  def mkLogger(name: String) : Logger[E]
}
