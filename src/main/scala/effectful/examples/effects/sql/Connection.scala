package effectful.examples.effects.sql

trait Connection {
  def url: String

  def isClosed : Boolean
}
