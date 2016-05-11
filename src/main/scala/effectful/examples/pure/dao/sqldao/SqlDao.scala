package effectful.examples.pure.dao.sqldao

import scala.language.higherKinds
import effectful._
import effectful.examples.effects.sql.{Connection, Cursor, SqlDriver, SqlVal}
import effectful.examples.pure.dao.Dao
import effectful.examples.pure.dao.Dao.RecordMetadata
import effectful.examples.pure.dao.query.Query

object SqlDao {
  case class FieldColumnMapping(
    fieldName: String,
    columnIndex: Int,
    columnName: String
  )
  case class RecordMapping[ID,A](
    fieldColumnMappings: Seq[FieldColumnMapping],
    idFieldName: String
  )(implicit
    val idToSql: ID => SqlVal,
    val sqlToId: SqlVal => ID,
    val recordFromSqlRow: Seq[SqlVal] => A,
    val sqlRowToRecord: Seq[SqlVal] => A
  ) {
    def fieldCount = fieldColumnMappings.size
  }

  implicit class StringPML(val self: String) extends AnyVal {
    def esc = s"`$self`"
  }
  implicit class StringContextPML(val self: StringContext) extends AnyVal {
    def esc(args: String*) = self.s(args.map(_.esc):_*)
  }
}

class SqlDao[ID,A,E[_]](
  sql: SqlDriver[E],
  tableName: String,
  metadataTableName: String
)(implicit
  E:EffectSystem[E],
  connection: Connection,
  recordMapping: SqlDao.RecordMapping[ID,A],
  metadataMapping: SqlDao.RecordMapping[ID,RecordMetadata]
) extends Dao[ID,A,E] {
  import SqlDao._
  import recordMapping._
  val metadataTableSameAsRecord = tableName == metadataTableName
  val fieldNames = fieldColumnMappings.map(_.fieldName).filterNot(_ == idFieldName)
  val metadataFieldNames = metadataMapping.fieldColumnMappings.map(_.fieldName)

  val sqlMaybeJoinMetadata =
    if(metadataTableSameAsRecord) {
      ""
    }  else {
      esc"LEFT JOIN $tableName ON $metadataTableName.$idFieldName=$metadataTableName.$idFieldName"
    }

  val qSelectRecordAndMetadata =
    s"SELECT ${idFieldName.esc},${
      (fieldNames ++ metadataFieldNames).map(_.esc).mkString(",")
    } FROM ${tableName.esc} $sqlMaybeJoinMetadata"

  def parseRecordAndMetadataCursor(cursor: Cursor) : Iterator[(ID,A,RecordMetadata)] = {
    cursor.map { row =>
      val idCol = row.head
      val (recordRow, metadataRow) = row.tail.toSeq.splitAt(fieldCount)
      (sqlToId(idCol), sqlRowToRecord(recordRow), metadataMapping.sqlRowToRecord(metadataRow))
    }
  }

  val qFindById =
      sql.prepare(
        s"$qSelectRecordAndMetadata WHERE ${idFieldName.esc} = ?"
      ).map { ps =>
        { id: ID =>
          for {
            cursor <- sql.executePreparedQuery(ps)(Seq(idToSql(id)))
          } yield {
            parseRecordAndMetadataCursor(cursor)
          }
        }
      }

  override def findById(id: ID): E[Option[(ID, A, RecordMetadata)]] =
    for {
      fq <- qFindById
      result <- fq(id)
    } yield {
      result.toStream.headOption
    }

  // todo: translate Query[A] to SQL where text
  def queryToSqlWhere(query: Query[A]) : String = ???

  override def find(query: Query[A]): E[Seq[(ID, A, RecordMetadata)]] =
    for {
      cursor <- sql.executeQuery(s"$qSelectRecordAndMetadata WHERE ${queryToSqlWhere(query)}")
    } yield parseRecordAndMetadataCursor(cursor).toSeq

  val qFindAll = sql.prepare(qSelectRecordAndMetadata)

  override def findAll(start: Int, batchSize: Int): E[Seq[(ID, A, RecordMetadata)]] =
    for {
      fq <- qFindAll
      cursor <- sql.executePreparedQuery(fq)()
    } yield parseRecordAndMetadataCursor(cursor).toSeq

  override def remove(id: ID): E[Boolean] = ???
  override def batchRemove(ids: TraversableOnce[ID]): E[Int] = ???

  override def insert(id: ID, a: A): E[Boolean] = ???
  override def batchInsert(records: Seq[(ID, A)]): E[Int] = ???

  override def update(id: ID, value: A): E[Boolean] = ???
  override def batchUpdate(records: TraversableOnce[(ID, A)]): E[Int] = ???

  override def batchUpsert(records: TraversableOnce[(ID, A)]): E[(Int, Int)] = ???
  override def upsert(id: ID, a: A): E[(Boolean, Boolean)] = ???
}
