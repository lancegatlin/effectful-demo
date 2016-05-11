package effectful.examples.effects.sql

case class ColumnMetadata(
  name: String,
  label: String,
  sqlType: SqlType,
  autoIncrement: Boolean,
  caseSensitive: Boolean,
  nullable: Boolean,
  signed: Boolean
)
