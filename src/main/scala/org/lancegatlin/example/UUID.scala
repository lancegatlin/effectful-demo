package org.lancegatlin.example

import scala.language.higherKinds

trait UUID extends Comparable[UUID] {
  // other std methods that must be implemented:
  // toString
  // equals
  // hashCode
}

trait UUIDService[E[_]] {
  def apply() : E[UUID]
}
