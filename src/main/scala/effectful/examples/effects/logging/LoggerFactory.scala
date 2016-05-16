package effectful.examples.effects.logging

import scala.language.higherKinds

trait LoggerFactory[E[+_]] {
  def mkLogger(name: String) : Logger[E]
}
