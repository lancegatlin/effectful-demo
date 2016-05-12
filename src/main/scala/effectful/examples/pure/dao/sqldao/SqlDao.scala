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
    val recordToSqlRow: A => Seq[SqlVal],
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

  lazy val qFindById =
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

  lazy val qFindAll = sql.prepare(qSelectRecordAndMetadata)

  override def findAll(start: Int, batchSize: Int): E[Seq[(ID, A, RecordMetadata)]] =
    for {
      fq <- qFindAll
      cursor <- sql.executePreparedQuery(fq)()
    } yield parseRecordAndMetadataCursor(cursor).toSeq

  lazy val qRemove = sql.prepare(esc"DELETE FROM $tableName WHERE $idFieldName=?").map { ps =>
    { id:ID =>
      for {
        result <- sql.executePreparedUpdate(ps)(Seq(idToSql(id)))
      } yield result == 1
    }
  }
  override def remove(id: ID): E[Boolean] =
    for {
      fq <- qRemove
      result <- fq(id)
    } yield result

  override def batchRemove(ids: Traversable[ID]): E[Int] = {
    for {
      updateCount <- sql.executeUpdate(
        esc"DELETE FROM $tableName WHERE $idFieldName IN (" +
        ids.map(id => idToSql(id).printSQL).mkString(",") +
        ")"
      )
    } yield updateCount
  }

  lazy val qInsert =
    sql.prepare(
      s"INSERT INTO ${tableName.esc} VALUES(${(0 until fieldCount).map(_ => "?").mkString(",")})"
    ).map { ps =>

      { values:Seq[(ID,A)] =>
        sql.executePreparedUpdate(ps)(values.map { case (id,a) =>
          idToSql(id) +: recordToSqlRow(a)
        }:_*)
      }
    }

  override def insert(id: ID, a: A): E[Boolean] =
    for {
      fq <- qInsert
      insertCount <- fq(Seq((id,a)))
    } yield insertCount == 1

  override def batchInsert(records: Traversable[(ID, A)]): E[Int] =
    for {
      fq <- qInsert
      insertCount <- fq(records.toSeq)
    } yield insertCount


  lazy val qUpdate =
    sql.prepare(
      s"UPDATE ${tableName.esc} SET ${fieldNames.map(name => esc"$name=?").mkString(",")} WHERE ${idFieldName.esc}=?"
    ).map { ps =>

      { (id:ID,a:A) =>
        sql.executePreparedUpdate(ps)(recordToSqlRow(a) :+ idToSql(id))
      }
    }
  override def update(id: ID, value: A): E[Boolean] =
    for {
      fq <- qUpdate
      updateCount <- fq(id,value)
    } yield updateCount == 1

  override def batchUpdate(records: Traversable[(ID, A)]): E[Int] =
    for {
      // todo: thread safety of this op? what if multiple callers call beginTransaction?
      _ <- sql.beginTransaction()
      results <- records.map { case (id,a) => update(id,a) }.sequence
      _ <- sql.commit()
    } yield results.count(_ == true)

  override def batchUpsert(records: Traversable[(ID, A)]): E[(Int, Int)] = ???
  override def upsert(id: ID, a: A): E[(Boolean, Boolean)] = ???
}
