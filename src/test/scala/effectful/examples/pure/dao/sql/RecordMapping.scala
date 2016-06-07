package effectful.examples.pure.dao.sql

case class FieldColumnMapping(
  fieldName: String,
  columnIndex: Int,
  columnName: ColName
)

//object FieldColumnMapping {
//  def apply(
//    fieldName: String,
//    columnIndex: Int,
//    columnName: String
//  ) : FieldColumnMapping =
//    FieldColumnMapping(
//      fieldName = fieldName,
//      columnIndex = columnIndex,
//      columnName = ColName(columnName)
//    )
//}

// todo: simple DDL generator
case class RecordMapping[ID,A](
  tableName: TableName,
  recordFields: Seq[FieldColumnMapping],
  idField: FieldColumnMapping
) {
  def recordFieldCount = recordFields.size
  val recordFieldsOrdered = recordFields.sortBy(_.columnIndex)
  val allFields = idField +: recordFields
  val allFieldsOrdered = allFields.sortBy(_.columnIndex)
}

//object RecordMapping {
//  def apply[ID,A](
//    tableName: String,
//    recordFields: Seq[FieldColumnMapping],
//    idField: FieldColumnMapping
//  ) : RecordMapping[ID,A] =
//    RecordMapping(
//      tableName = TableName(tableName),
//      recordFields = recordFields,
//      idField = idField
//    )
//}