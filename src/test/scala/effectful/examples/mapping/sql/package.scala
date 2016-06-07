package effectful.examples.mapping

import effectful.examples.effects.sql.SqlDriver.SqlRow
import effectful.examples.effects.sql._
import effectful.examples.pure.dao.DocDao.RecordMetadata
import effectful.examples.pure.dao.sql._
import effectful.examples.pure.dao.sql.{FieldColumnMapping, RecordMapping}
import effectful.examples.pure.user.Tokens.TokenInfo
import effectful.examples.pure.user.impl.UsersImpl.UserData
import effectful.examples.pure.uuid.UUIDs
import effectful.examples.pure.uuid.UUIDs.UUID

package object sql {
  import SqlVal._

  implicit def sqlPrint_UUID[E[_]](implicit uuids:UUIDs[E]) = new PrintSql[UUID] {
    override def printSql(uuid: UUID): SqlString =
      uuids.toBase64(uuid).sql
  }

  implicit def charDataFormat_UUID[E[_]](implicit uuids:UUIDs[E]) = new CharDataFormat[UUID] {
    def toCharData(uuid: UUID) =
      CharData(uuids.toBase64(uuid))

    def fromCharData(data: CharData) =
      uuids.fromBase64(data.toCharString()).getOrElse(
        throw new RuntimeException("Failed to parse UUID from Base64")
      )
  }

  implicit def sqlRowFormat_RecordMetadata[E[_]](implicit uuids:UUIDs[E]) = new SqlRowFormat[RecordMetadata] {
    def toSqlRow(a: RecordMetadata) = {
      import a._

      IndexedSeq(
        TIMESTAMP(created),
        TIMESTAMP(lastUpdated),
        // todo: better way to do this
        removed.map(TIMESTAMP).orSqlNull(SqlType.TIMESTAMP)
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

  // TokenInfo

  implicit def sqlRecordFormat_TokenInfo[E[_]](implicit uuids:UUIDs[E]) = new SqlRecordFormat[String,TokenInfo] {

    def toSqlVal(a: String) =
      SqlVal.VARCHAR(30,CharData(a))

    def fromSqlVal(v: SqlVal) =
      v.as[VARCHAR].data.toCharString()

    def toSqlRow(a: TokenInfo) = {
      import a._

      IndexedSeq(
        VARCHAR(30,userId.toCharData),
        // todo: better way to do this
        deviceId.map(uuid => VARCHAR(30,uuid.toCharData)).orSqlNull(SqlType.VARCHAR(30)),
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

  implicit def sqlRecordFormat_TokenInfo_RecordMetadata[E[_]](implicit uuids:UUIDs[E]) =
    SqlRecordFormat(
      idFormat = sqlRecordFormat_TokenInfo,
      rowFormat = sqlRowFormat_RecordMetadata
    )

  // todo: make a macro to generate this
  val tokenInfoRecordMapping = RecordMapping[String,TokenInfo](
    tableName = "Tokens",
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
      columnName = ColName("token")
    )
  )


  // todo: make a macro to generate this
  val tokenInfoMetadataRecordMapping = RecordMapping[String,RecordMetadata](
    tableName = "Tokens",
    recordFields = Seq(
      FieldColumnMapping(
        fieldName = "created",
        columnIndex = 6,
        columnName = ColName("created")
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
  )

  // UserData

  implicit def sqlRecordFormat_UserData[E[_]](implicit uuids:UUIDs[E]) = new SqlRecordFormat[UUID,UserData] {

    def toSqlVal(a: UUID) =
      SqlVal.VARCHAR(30,a.toCharData)

    def fromSqlVal(v: SqlVal) =
      v.as[VARCHAR].data.to[UUID]

    def toSqlRow(a: UserData) = {
      import a._

      IndexedSeq(
        VARCHAR(255,username.toCharData),
        VARCHAR(255,passwordDigest.toCharData)
      )
    }

    def fromSqlRow(row: SqlRow) = {
      UserData(
        username = row(0).as[VARCHAR].data.toCharString(),
        passwordDigest = row(1).as[VARCHAR].data.toCharString()
      )
    }
  }

  implicit def sqlRecordFormat_UserData_RecordMetadata[E[_]](implicit uuids:UUIDs[E]) =
    SqlRecordFormat(
      idFormat = sqlRecordFormat_UserData,
      rowFormat = sqlRowFormat_RecordMetadata
    )

  val userDataRecordMapping = RecordMapping[UUID,UserData](
    tableName = "Users",
    recordFields = Seq(
      FieldColumnMapping(
        fieldName = "username",
        columnIndex = 3,
        columnName = "username"
      ),
      FieldColumnMapping(
        fieldName = "passwordDigest",
        columnIndex = 4,
        columnName = "password_digest"
      )
    ),
    idField = FieldColumnMapping(
      fieldName = "id",
      columnIndex = 1,
      columnName = "id"
    )
  )

  // todo: make a macro to generate this
  val userDataMetadataRecordMapping = RecordMapping[UUID,RecordMetadata](
    tableName = "Users",
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
      fieldName = "id",
      columnIndex = 1,
      columnName = "id"
    )
  )


}
