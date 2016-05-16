package effectful.examples.pure.dao.sqldao

import scala.language.higherKinds
import effectful._
import effectful.examples.effects.sql._
import effectful.examples.pure.dao.Dao
import effectful.examples.pure.dao.Dao.RecordMetadata
import effectful.examples.pure.dao.query.Query
import SqlDriver._

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
    val recordToSqlRow: A => IndexedSeq[SqlVal],
    val sqlRowToRecord: IndexedSeq[SqlVal] => A
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
  connection: ConnectionPool,
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
      esc"LEFT JOIN $metadataTableName ON $tableName.$idFieldName=$metadataTableName.$idFieldName"
    }

  val allFieldNames = fieldNames ++ metadataFieldNames

  val qSelectRecordAndMetadata =
    s"SELECT ${idFieldName.esc},${
      allFieldNames.map(_.esc).mkString(",")
    } FROM ${tableName.esc} $sqlMaybeJoinMetadata"

  def parse : SqlRow => (ID,A,RecordMetadata) = { row =>
    val idCol = row.head
    val (recordRow, metadataRow) = row.tail.splitAt(fieldCount)
    (sqlToId(idCol), sqlRowToRecord(recordRow), metadataMapping.sqlRowToRecord(metadataRow))
  }

  lazy val qFindById =
      sql.prepare(
        s"$qSelectRecordAndMetadata WHERE ${idFieldName.esc} = ?"
      ).map { ps =>
        { id: ID =>
          sql.iteratePreparedQuery(ps)(IndexedSeq(idToSql(id))).map(parse)
        }
      }

  override def findById(id: ID): E[Option[(ID, A, RecordMetadata)]] =
    for {
      fq <- qFindById
      iterator = fq(id)
      result <- iterator.headOption()
    } yield result

  // todo: translate Query[A] to SQL where text
  def queryToSqlWhere(query: Query[A]) : String = ???

  override def find(query: Query[A]): E[Seq[(ID, A, RecordMetadata)]] =
    sql.iterateQuery(s"$qSelectRecordAndMetadata WHERE ${queryToSqlWhere(query)}")
      .map(parse)
      .collect[IndexedSeq]().widen

  lazy val qFindAll = sql.prepare(qSelectRecordAndMetadata)

  override def findAll(start: Int, batchSize: Int): E[Seq[(ID, A, RecordMetadata)]] =
    for {
      fq <- qFindAll
      result <- sql.iteratePreparedQuery(fq)().map(parse).collect[IndexedSeq]
    } yield result

  lazy val qExists =
    sql.prepare(
      s"SELECT 1 FROM ${tableName.esc} WHERE ${idFieldName.esc}=?"
    ).map { ps =>

      { id:ID =>
        for {
          cursor <- sql.executePreparedQuery(ps)(IndexedSeq(idToSql(id)))
        } yield {
          cursor.nonEmpty
        }
      }
    }

  override def exists(id: ID): E[Boolean] =
    for {
      fq <- qExists
      result <- fq(id)
    } yield result

  override def batchExists(ids: Traversable[ID]): E[Set[ID]] =
    sql.iterateQuery(
      s"SELECT ${idFieldName.esc} FROM ${tableName.esc} WHERE ${idFieldName.esc} IN (${ids.map(id => idToSql(id).printSQL).mkString(",")}) "
    ).map(row => sqlToId(row(0))).collect[Set]


  // todo: fix me this should be an update to metadata not actual delete
  lazy val qRemove = sql.prepare(esc"DELETE FROM $tableName WHERE $idFieldName=?").map { ps =>
    { id:ID =>
      for {
        result <- sql.executePreparedUpdate(ps)(IndexedSeq(idToSql(id)))
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

  // todo: insert to metadata table too if needed
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


  // todo: update metadata table too
  // todo: this should be def that accepts implicit context to allow binding transaction below
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
    // Note: implicit keyword not currently allowed inside for-compre https://issues.scala-lang.org/browse/SI-2823
    sql.beginTransaction().flatMap { implicit transaction =>
      for {
        // todo: this doesn't bind the transaction properly
        results <- records.map { case (id,a) => update(id,a) }.sequence
        _ <- sql.commit()
      } yield results.count(_ == true)
    }

  override def upsert(id: ID, a: A): E[(Boolean, Boolean)] =
    for {
      // todo: maybe faster to just update and see if it fails?
      idExists <- exists(id)
      result <- if(idExists) {
        update(id,a).map(result => (false,result))
      } else {
        insert(id,a).map(result => (result,false))
      }
    } yield result

  override def batchUpsert(records: Traversable[(ID, A)]): E[(Int, Int)] =
    for {
      // todo: maybe faster to just update and see if it fails?
      idToExists <- batchExists(records.map(_._1))
      (toUpdate,toInsert) = records.partition { case (id,a) => idToExists(id) }
      eUpdates = batchUpdate(toUpdate)
      eInserts = batchInsert(toInsert)
      updateCount <- eUpdates
      insertCount <- eInserts
    } yield (insertCount,updateCount)
}
