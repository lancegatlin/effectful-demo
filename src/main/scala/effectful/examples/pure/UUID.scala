package effectful.examples.pure

import scala.language.higherKinds

trait UUID extends Comparable[UUID] {
  // other std methods that must be implemented:
  // toString
  // equals
  // hashCode
}

trait UUIDService[E[_]] {
  def gen() : E[UUID]
  def parse(s: String) : E[Option[UUID]]
}
