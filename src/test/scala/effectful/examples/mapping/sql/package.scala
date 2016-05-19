package effectful.examples.mapping

import effectful.examples.effects.sql.SqlDriver.SqlRow
import effectful.examples.effects.sql._
import effectful.examples.pure.UUIDService.UUID
import effectful.examples.pure.dao.DocDao.RecordMetadata
import effectful.examples.pure.dao.sql.SqlDocDao.{FieldColumnMapping, RecordMapping}
import effectful.examples.pure.user.TokenService.TokenInfo
import org.apache.commons.codec.binary.Base64

package object sql {
  import SqlVal._

  def failedToParse(sqlVal: SqlVal) =
    new IllegalArgumentException(s"Failed to parse $sqlVal")

  implicit val sqlValToString : SqlVal => String = {
    case CHAR(_,data) => data.toCharString()
    case NCHAR(_,data) => data.toCharString()
    case VARCHAR(_,data) => data.toCharString()
    case NVARCHAR(_,data) => data.toCharString()
    case v => throw failedToParse(v)
  }

  implicit val stringToSqlVal : String => SqlVal = { s =>
    // todo: think about how to write string data - no knowledge of exact column string type here
    SqlVal.NVARCHAR(0,CharData(s))
  }

  implicit val sqlValToUUID : SqlVal => UUID = { v =>
     UUID(Base64.decodeBase64(v.fromSql[String]))
  }

  implicit val uuidToSqlVal : UUID => SqlVal = { v =>
    Base64.encodeBase64String(v.bytes).toSql
  }

  implicit def sqlValToOption[A](implicit f: SqlVal => A) : SqlVal => Option[A] = {
    case NULL(_) => None
    case v => Some(f(v))
  }

  implicit def optionToSqlVal[A](implicit f: A => SqlVal) : Option[A] => SqlVal = {
    case Some(a) => f(a)
    case None =>
      // todo: how to handle this properly - don't know intended sql type here?
      val sqlType : SqlType = _
      NULL(sqlType)
  }

  implicit val sqlValToInstant : SqlVal => java.time.Instant = {
    case TIMESTAMP(instant) => instant
    case v => throw failedToParse(v)
  }

  implicit val instantToSqlVal : java.time.Instant => SqlVal = { v =>
    TIMESTAMP(v)
  }

  implicit val sqlRowToTokenInfo : SqlRow => TokenInfo = { row =>
    TokenInfo(
      userId = row(0).fromSql[UUID],
      deviceId = row(1).fromSql[Option[UUID]],
      lastValidated = row(2).fromSql[java.time.Instant],
      expiresOn = row(3).fromSql[java.time.Instant]
    )
  }

  implicit val tokenInfoToSqlRow : TokenInfo => SqlRow = { v =>
    import v._

    IndexedSeq(
      userId.toSql,
      deviceId.toSql,
      lastValidated.toSql,
      expiresOn.toSql
    )
  }

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
  )

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
  )

}
