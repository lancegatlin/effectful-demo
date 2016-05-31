package effectful.examples

import effectful._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.slf4j.Slf4jLogger
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.uuid.UUIDs.UUID

import scala.concurrent.duration._

object IdExample {

  val uuids = new JavaUUIDs

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuids
  )

  val tokenDao = new SqlDocDao[String,Tokens.TokenInfo,Id](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )


  val tokens = new TokensImpl[Id](
    logger = Slf4jLogger("tokens").liftService,
    uuids = uuids.liftService,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

  val passwords = new PasswordsImpl[Id](
    passwordMismatchDelay = 5.seconds
  )

  val userDao = new SqlDocDao[UUID,UsersImpl.UserData,Id](
    sql = sqlDriver.liftService,
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val users = new UsersImpl[Id](
    users = userDao,
    passwords = passwords
  )

  val userLogins = new UserLoginsImpl[Id](
    logger = Slf4jLogger("userLogins").liftService,
    users = users,
    tokens = tokens,
    passwords = passwords
  )
}
