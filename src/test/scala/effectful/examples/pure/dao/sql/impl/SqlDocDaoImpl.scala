package effectful.examples.pure.dao.sql.impl


import java.time.Instant

import effectful._
import effectful.examples.effects.sql._
import effectful.examples.pure.dao.DocDao.RecordMetadata
import SqlDriver._
import effectful.augments._
import cats.Monad
import effectful.examples.pure.dao.sql._

class SqlDocDaoImpl[ID,A,E[_]](
  sql: SqlDriver[E],
  val recordMapping: RecordMapping[ID,A],
  val metadataMapping: RecordMapping[ID,RecordMetadata]
)(implicit
  E:Monad[E],
  P:Par[E],
  X:Exceptions[E],
  val recordFormat: SqlRecordFormat[ID,A],
  val metadataFormat: SqlRecordFormat[ID,RecordMetadata],
  val sqlPrint_ID: PrintSql[ID]
) extends SqlDocDao[ID,A,E] {
  import Monad.ops._
  import recordMapping._

  val metadataTableName = metadataMapping.tableName
  val metadataTableSameAsRecord = tableName == metadataTableName

//  val recordFieldNames = recordFields.map(_.fieldName)
  val recordColNames = recordFields.map(_.columnName)

  val meta_recordFieldNames =
    metadataMapping.recordFields.map(_.fieldName)

  val recordFieldNameToColName =
    recordFields.map(m => (m.fieldName,m.columnName)).toMap
  val meta_recordFieldNameToColName =
    metadataMapping.recordFields.map(m => (m.fieldName,m.columnName)).toMap

  val idColName = idField.columnName

  val sqlMaybeJoinMetadata =
    if(metadataTableSameAsRecord) {
      sql""
    }  else {
      sql"LEFT JOIN $metadataTableName ON $tableName.$idColName=$metadataTableName.$idColName"
    }

//  val allFieldNames =
//    Seq(idFieldName) ++
//    dataFieldNames.filterNot(_ == idFieldName) ++
//    meta_dataFieldNames

  // both record & metadata
  val selectAllRecordCols = {
    recordFieldsOrdered.map(f => sql"$tableName.${f.columnName}") ++
    // leave out metadata table id column
    metadataMapping.recordFieldsOrdered.map(f => sql"$metadataTableName.${f.columnName}")
  }.mkSqlString(",")

  val qSelectFullRecord =
    sql"SELECT $tableName.$idColName,$selectAllRecordCols FROM $tableName $sqlMaybeJoinMetadata"

  val removedColName = meta_recordFieldNameToColName("removed")
  val lastUpdatedColName = meta_recordFieldNameToColName("lastUpdated")

  def parseFullRecord : SqlRow => (ID,A,RecordMetadata) = { row =>
    val idCol = row.head
    val (recordRow, metadataRow) = row.tail.splitAt(recordFieldCount)
    (recordFormat.fromSqlVal(idCol), recordFormat.fromSqlRow(recordRow), metadataFormat.fromSqlRow(metadataRow))
  }

  def prepFindById(implicit context: SqlDriver.Context): E[ID => E[Option[(ID,A,RecordMetadata)]]]  =
      sql.prepare(
        sql"$qSelectFullRecord WHERE $idColName=?"
      ).map { ps =>
        { (id:ID) =>
            sql.iteratePreparedQuery(ps)(IndexedSeq(recordFormat.toSqlVal(id)))
              .map(parseFullRecord)
              .headOption()
        }
      }

  override def findById(id: ID): E[Option[(ID, A, RecordMetadata)]] =
    sql.autoCommit { implicit autoCommit =>
      prepFindById.flatMap(_(id))
    }

  override def findByNativeQuery(whereClause: SqlString): E[Seq[(ID, A, RecordMetadata)]] =
    sql.autoCommit { implicit autoCommit =>
      sql.iterateQuery(sql"$qSelectFullRecord WHERE $whereClause")
        .map(parseFullRecord)
        .collect[IndexedSeq]()
        .widen
    }

  def prepFindAll(implicit context: Context) : E[() => E[IndexedSeq[(ID,A,RecordMetadata)]]] =
    sql.prepare(qSelectFullRecord).map { ps =>
      { () =>
        sql.iteratePreparedQuery(ps)()
          .map(parseFullRecord)
          .collect[IndexedSeq]
      }
    }

  override def findAll(start: Int, batchSize: Int): E[Seq[(ID, A, RecordMetadata)]] =
    sql.autoCommit { implicit autoCommit =>
      prepFindAll.flatMap(_()).widen
    }

  def prepExists(implicit context: Context) : E[ID => E[Boolean]] =
    sql.prepare(
      sql"SELECT 1 FROM $tableName WHERE $idColName=?"
    ).map { ps =>
      { (id:ID) =>
        for {
          cursor <- sql.executePreparedQuery(ps)(IndexedSeq(recordFormat.toSqlVal(id)))
        } yield cursor.nonEmpty
      }
    }

  override def exists(id: ID): E[Boolean] =
    sql.autoCommit { implicit autoCommit =>
      prepExists.flatMap(_(id))
    }

  override def batchExists(ids: Traversable[ID]): E[Set[ID]] =
    sql.autoCommit { implicit autoCommit =>
      sql.iterateQuery(
        sql"SELECT $idColName FROM $tableName WHERE $idColName IN (${ids.map(_.printSql).mkSqlString(",")}) "
      )
        .map(row => recordFormat.fromSqlVal(row(0)))
        .collect[Set]
    }


  def prepMarkRemoved(implicit context: Context) : E[ID => E[Boolean]] =
    sql.prepare(
      sql"UPDATE $metadataTableName SET $removedColName=? WHERE $idColName=?"
    ).map { ps =>

      { (id:ID) =>
        sql.executePreparedUpdate(ps)(
          IndexedSeq(SqlVal.TIMESTAMP(Instant.now()),recordFormat.toSqlVal(id))
        ).map(_ == 1)
      }
    }

  override def remove(id: ID): E[Boolean] =
    sql.autoCommit { implicit autoCommit =>
      prepMarkRemoved.flatMap(_(id))
    }

  override def batchRemove(ids: Traversable[ID]): E[Int] = {
    sql.inTransaction { implicit transaction =>
      for {
        _remove <- prepMarkRemoved
        updateCounts <- ids.map(_remove).sequence
      } yield updateCounts.count(_ == true)
    }
  }

  val newMetadata = RecordMetadata.forNewRecord

  val allFieldCount = recordMapping.recordFieldCount + 1 + metadataMapping.recordFieldCount
  val allFieldsOrdered = (recordMapping.allFields ++ metadataMapping.recordFields).sortBy(_.columnIndex)

  val allFieldQuestionMarks = `repeat_?`(allFieldCount).mkSqlString(",")

  def prepInsert(implicit context: Context) : E[Seq[(ID,A)] => E[Int]] =
    if(metadataTableSameAsRecord) {
      sql.prepare(
        sql"INSERT INTO $tableName (${allFieldsOrdered.map(_.columnName).mkSqlString(",")}) VALUES($allFieldQuestionMarks)"
      ).map { ps =>

        { (values:Seq[(ID,A)]) =>
          sql.executePreparedUpdate(ps)(values.map { case (id,a) =>
            (recordFormat.toSqlVal(id) +: recordFormat.toSqlRow(a)) ++ metadataFormat.toSqlRow(newMetadata)
          }:_*)
        }
      }
    } else {
      // Maybe run in parallel
      for {
        tuple <- E.par(
          sql.prepare(
            sql"INSERT INTO $tableName (${recordMapping.allFieldsOrdered.map(_.columnName).mkSqlString(",")}) VALUES(${`repeat_?`(recordFieldCount + 1).mkSqlString(",")})"
          ),
          sql.prepare(
            sql"INSERT INTO $metadataTableName (${metadataMapping.allFieldsOrdered.map(_.columnName).mkSqlString(",")}) VALUES(${`repeat_?`(metadataMapping.recordFieldCount + 1).mkSqlString(",")}"
          )
        )
        (prepMainInsert,prepMetadataInsert) = tuple
      } yield { (values: Seq[(ID, A)]) =>
        // Maybe run in parallel
        for {
          tuple <- E.par(
              sql.executePreparedUpdate(prepMainInsert)(values.map { case (id,a) =>
              recordFormat.toSqlVal(id) +: recordFormat.toSqlRow(a)
            }:_*),
            sql.executePreparedUpdate(prepMetadataInsert)(values.map { case (id,a) =>
              recordFormat.toSqlVal(id) +: metadataFormat.toSqlRow(newMetadata)
            }:_*)
          )
          (mainInsertCount,metadataInsertCount) = tuple
        } yield mainInsertCount
      }
    }

  override def insert(id: ID, a: A): E[Boolean] =
    sql.inTransaction { implicit transaction =>
      prepInsert.flatMap(_(Seq((id,a)))).map(_ == 1)
    }

  override def batchInsert(records: Traversable[(ID, A)]): E[Int] =
    sql.inTransaction { implicit transaction =>
      prepInsert.flatMap(_(records.toSeq))
    }


  // todo: update metadata table too
  // todo: this should be def that accepts implicit context to allow binding transaction below
  def prepUpdate(implicit context: Context) : E[(ID,A) => E[Boolean]] =
    if(metadataTableSameAsRecord) {
      sql.prepare(
        sql"UPDATE $tableName SET ${recordColNames.map(name => sql"$name=?").mkSqlString(",")},$lastUpdatedColName=? WHERE $idColName=?"
      ).map { ps =>

        { (id:ID,a:A) =>
          sql.executePreparedUpdate(ps)(
            recordFormat.toSqlRow(a) ++
            IndexedSeq(SqlVal.TIMESTAMP(Instant.now()),recordFormat.toSqlVal(id))
          ).map(_ == 1)
        }
      }
    } else {
      for {
        tuple <- E.par(
          sql.prepare(
            s"UPDATE $tableName SET ${recordColNames.map(name => sql"$name=?").mkSqlString(",")} WHERE $idColName=?"
          ),
          sql.prepare(
            s"UPDATE $metadataTableName SET $lastUpdatedColName=? WHERE $idColName=?"
          )
        )
        (prepMainUpdate,prepMetaUpdate) = tuple
      } yield { (id:ID,a:A) =>
        for {
          mainUpdateCount <- sql.executePreparedUpdate(prepMainUpdate)(
            recordFormat.toSqlRow(a) :+ recordFormat.toSqlVal(id)
          )
          metaUpdateCount <- sql.executePreparedUpdate(prepMetaUpdate)(
            IndexedSeq(SqlVal.TIMESTAMP(Instant.now()),recordFormat.toSqlVal(id))
          )
        } yield mainUpdateCount == 1
      }
    }


  override def update(id: ID, value: A): E[Boolean] =
    sql.autoCommit { implicit autoCommit =>
      prepUpdate.flatMap(_(id,value))
    }

  override def batchUpdate(records: Traversable[(ID, A)]): E[Int] =
    // Note: implicit keyword not currently allowed inside for-compre https://issues.scala-lang.org/browse/SI-2823
    sql.inTransaction { implicit transaction =>
      for {
        _update <- prepUpdate
        results <- records.map { case (id,a) => _update(id,a) }.sequence
      } yield results.count(_ == true)
    }

  override def upsert(id: ID, a: A): E[(Boolean, Boolean)] =
    for {
      // todo: maybe faster to just update and see if it fails?
      idExists <- exists(id)
      result <-
        if(idExists) {
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
      result <- sql.inTransaction { implicit transaction =>
        for {
          _update <- prepUpdate
          _insert <- prepInsert
          result <- {
            val eUpdates = toUpdate.map { case (id,a) => _update(id,a) }.sequence.map(_.count(_ == true))
            val eInserts = _insert(toInsert.toSeq)
            for {
              updateCount <- eUpdates
              insertCount <- eInserts
            } yield (insertCount,updateCount)
          }
        } yield result
      }
    } yield result
}
