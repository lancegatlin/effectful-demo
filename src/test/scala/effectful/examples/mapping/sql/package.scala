package effectful.examples.mapping

import org.apache.commons.codec.binary.Base64
import effectful.examples.effects.sql.SqlDriver.SqlRow
import effectful.examples.effects.sql._
import effectful.examples.pure.UUIDService.UUID
import effectful.examples.pure.dao.DocDao.RecordMetadata
import effectful.examples.pure.dao.sql._
import effectful.examples.pure.dao.sql.SqlDocDao.{FieldColumnMapping, RecordMapping}
import effectful.examples.pure.user.TokenService.TokenInfo

package object sql {
  import SqlVal._

  implicit val charDataFormat_UUID = new CharDataFormat[UUID] {
    def toCharData(a: UUID) =
      CharData(Base64.encodeBase64String(a.bytes))

    def fromCharData(data: CharData) =
      UUID(Base64.decodeBase64(data.toCharString()))
  }

  val sqlRecordFormat_TokenInfo = new SqlRecordFormat[String,TokenInfo] {

    def toSqlVal(a: String) =
      SqlVal.VARCHAR(30,CharData(a))

    def fromSqlVal(v: SqlVal) =
      v.as[VARCHAR].data.toCharString()

    def toSqlRow(a: TokenInfo) = {
      import a._

      IndexedSeq(
        VARCHAR(30,userId.toCharData),
        deviceId.map(uuid => VARCHAR(30,uuid.toCharData)).orSqlNull,
        TIMESTAMP(lastValidated),
        TIMESTAMP(expiresOn)
      )
    }

    def fromSqlRow(row: SqlRow) = {
      TokenInfo(
        userId = row(0).as[VARCHAR].data.to[UUID],
        deviceId = row(1).asNullable[VARCHAR].map(_.data.to[UUID]),
        lastValidated = row(2).as[TIMESTAMP].timestamp,
        expiresOn = row(3).as[TIMESTAMP].timestamp
      )
    }
  }

  // todo: make a macro to generate this
  val tokenInfoRecordMapping = RecordMapping[String,TokenInfo](
    tableName = "tokens",
    recordFields = Seq(
      FieldColumnMapping(
        fieldName = "userId",
        columnIndex = 2,
        columnName = "user_id"
      ),
      FieldColumnMapping(
        fieldName = "deviceId",
        columnIndex = 3,
        columnName = "device_id"
      ),
      FieldColumnMapping(
        fieldName = "lastValidated",
        columnIndex = 4,
        columnName = "last_validated"
      ),
      FieldColumnMapping(
        fieldName = "expiresOn",
        columnIndex = 5,
        columnName = "expires_on"
      )
    ),
    idField = FieldColumnMapping(
      fieldName = "token",
      columnIndex = 1,
      columnName = "token"
    )
  )(sqlRecordFormat_TokenInfo)

  val sqlRowFormat_RecordMetadata = new SqlRowFormat[RecordMetadata] {
    def toSqlRow(a: RecordMetadata) = {
      import a._

      IndexedSeq(
        TIMESTAMP(created),
        TIMESTAMP(lastUpdated),
        removed.map(TIMESTAMP).orSqlNull
      )
    }

    def fromSqlRow(row: SqlRow) = {
      RecordMetadata(
        created = row(0).as[TIMESTAMP].timestamp,
        lastUpdated = row(1).as[TIMESTAMP].timestamp,
        removed = row(2).asNullable[TIMESTAMP].map(_.timestamp)
      )
    }
  }

  // todo: make a macro to generate this
  val tokenInfoMetadataRecordMapping = RecordMapping[String,RecordMetadata](
    tableName = "tokens",
    recordFields = Seq(
      FieldColumnMapping(
        fieldName = "created",
        columnIndex = 6,
        columnName = "created"
      ),
      FieldColumnMapping(
        fieldName = "lastUpdated",
        columnIndex = 7,
        columnName = "last_updated"
      ),
      FieldColumnMapping(
        fieldName = "removed",
        columnIndex = 8,
        columnName = "removed"
      )
    ),
    idField = FieldColumnMapping(
      fieldName = "token",
      columnIndex = 1,
      columnName = "token"
    )
  )(SqlRecordFormat(
    idFormat = sqlRecordFormat_TokenInfo,
    rowFormat = sqlRowFormat_RecordMetadata
  ))

}
