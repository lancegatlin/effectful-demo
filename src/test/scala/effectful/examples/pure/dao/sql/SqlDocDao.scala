package effectful.examples.pure.dao.sql

import effectful.examples.pure.dao.DocDao
import effectful.examples.pure.dao.DocDao.RecordMetadata

trait SqlDocDao[ID,A,E[_]] extends DocDao[ID,A,SqlString,E] {

  val recordMapping: RecordMapping[ID,A]
  val metadataMapping: RecordMapping[ID,RecordMetadata]

  implicit val recordFormat: SqlRecordFormat[ID,A]
  implicit val sqlPrint_ID: PrintSql[ID]
  implicit val metadataFormat: SqlRecordFormat[ID,RecordMetadata]
}
