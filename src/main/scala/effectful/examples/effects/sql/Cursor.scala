package effectful.examples.effects.sql

trait Cursor extends Iterator[CursorRow] {
  def schemaName: String
  def tableName: String

  def columnCount : Int

  def columns(i: Int) : ColumnMetadata

  def seekAbsolute(rowNum: Int) : Unit
  def seekRelative(rowOffset: Int) : Unit

  def isBeforeFirst : Boolean
  def isFirst : Boolean

  def first() : Unit
  def last() : Unit
  def currentRowNum : Int
  def current: CursorRow

  def reverse() : Unit

  def isClosed : Boolean
  def close() : Unit
}

