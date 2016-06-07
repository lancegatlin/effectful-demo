package effectful.examples

import effectful._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.slf4j.Slf4jLogger
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.dao.sql.impl.SqlDocDaoImpl
import effectful.examples.pure.uuid.UUIDs.UUID

import scala.concurrent.duration._

object IdExample {

  implicit val uuids = new JavaUUIDs

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuids
  )

  val tokensDao = new SqlDocDaoImpl[String,Tokens.TokenInfo,Id](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )


  val tokens = new TokensImpl[Id](
    uuids = uuids.liftService,
    tokensDao = tokensDao,
    tokenDefaultDuration = 10.days,
    logger = Slf4jLogger("tokens")
  )

  val passwords = new PasswordsImpl[Id](
    passwordMismatchDelay = 5.seconds,
    logger = Slf4jLogger("passwords")
  )

  val userDao = new SqlDocDaoImpl[UUID,UsersImpl.UserData,Id](
    sql = sqlDriver.liftService,
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
