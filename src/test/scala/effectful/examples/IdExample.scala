package effectful.examples

import scala.concurrent.duration._
import cats._
import effectful._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.slf4j.Slf4jLogger
import effectful.examples.effects.sql.SqlDriver
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure.dao.sql.impl.SqlDocDaoImpl
import effectful.examples.pure.uuid.UUIDs
import effectful.examples.pure.uuid.UUIDs.UUID


object IdExample {

  implicit val uuids : UUIDs[Id] = new JavaUUIDs

  val sqlDriver : SqlDriver[Id] = new JdbcSqlDriver(
    getConnectionFromPool = () => SqlDb.pool.getConnection(),
    uuids = uuids
  )

  val tokensDao = new SqlDocDaoImpl[String,Tokens.TokenInfo,Id](
    sql = sqlDriver,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokens = new TokensImpl[Id](
    uuids = uuids,
    tokensDao = tokensDao,
    tokenDefaultDuration = 10.days,
    logger = Slf4jLogger("tokens")
  )

  val passwords = new PasswordsImpl[Id](
    passwordMismatchDelay = 5.seconds,
    logger = Slf4jLogger("passwords")
  )

  val userDao = new SqlDocDaoImpl[UUID,UsersImpl.UserData,Id](
    sql = sqlDriver,
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val users = new UsersImpl[Id](
    usersDao = userDao,
    passwords = passwords,
    logger = Slf4jLogger("users")
  )

  val userLogins = new UserLoginsImpl[Id](
    users = users,
    tokens = tokens,
    passwords = passwords,
    logger = Slf4jLogger("userLogins")
  )
}
