package effectful.examples

import effectful._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.slf4j.Slf4jLogger
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.uuid.impl.JavaUUIDService
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.uuid.UUIDService.UUID

import scala.concurrent.duration._

object IdExample {

  val uuidService = new JavaUUIDService

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuidService
  )

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,Id](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )


  val tokenService = new TokenServiceImpl[Id](
    logger = Slf4jLogger("tokenService").liftService,
    uuids = uuidService.liftService,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

  val passwordService = new PasswordServiceImpl[Id](
    passwordMismatchDelay = 5.seconds
  )

  val userDao = new SqlDocDao[UUID,UserServiceImpl.UserData,Id](
    sql = sqlDriver.liftService,
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val userService = new UserServiceImpl[Id](
    users = userDao,
    passwordService = passwordService
  )

  val userLoginService = new UserLoginServiceImpl[Id](
    logger = Slf4jLogger("userLoginService").liftService,
    users = userService,
    tokens = tokenService,
    passwords = passwordService
  )
}
