package effectful.examples.effects.sql

trait CursorRow extends (Int => SqlVal) with Iterable[SqlVal] {
  def columnCount : Int
  def iterator: Iterator[SqlVal]
}
