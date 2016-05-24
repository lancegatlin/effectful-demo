package effectful.examples

import effectful._
import effectful.examples.effects.logging.free.{FreeLogger, LoggingCmd}
import effectful.examples.effects.par.impl.FakeParSystem
import effectful.examples.effects.sql.free.{FreeSqlDriver, SqlDriverCmd}
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.TokenService
import effectful.examples.mapping.sql._
import effectful.examples.pure.user.impl.TokenServiceImpl

import scalaz.{-\/, \/, \/-}
import scala.concurrent.duration._


object FreeMonadExample {

  type Cmd[A] = LoggingCmd[A] \/ SqlDriverCmd[A]
  type E[A] = Free[Cmd,A]

  implicit val fakeParSystem = new FakeParSystem[E]

  // todo: generalize these
  implicit val liftCmd_LoggingCmd_Cmd = new LiftCmd[LoggingCmd,Cmd] {
    override def apply[AA](cmd: LoggingCmd[AA]): Cmd[AA] =
      -\/(cmd)
  }

  implicit val liftCmd_FreeLoggingCmd_Cmd = new LiftCmd[SqlDriverCmd,Cmd] {
    override def apply[AA](cmd: SqlDriverCmd[AA]): Cmd[AA] =
      \/-(cmd)
  }

  //  implicit val effectSystem_E = Nested[Future,LogWriter]
  //  implicit val fakeParSystem = new FakeParSystem[E]


  val uuidService = new JavaUUIDService

  val sqlDriver = new FreeSqlDriver

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,E](
    sql = sqlDriver.liftS,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokenService = new TokenServiceImpl[E](
    logger = new FreeLogger("tokenService").liftS,
    uuids = uuidService.liftS,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

  tokenService.find("asdf")
//  val delayService = new AsyncDelayService()
//
//  val passwordService = new PasswordServiceImpl[E](
//    delayService = delayService.liftS,
//    passwordMismatchDelay = 5.seconds
//  )
//
//  val userDao = new SqlDocDao[UUID,UserServiceImpl.UserData,E](
//    sql = sqlDriver.liftS,
//    recordMapping = userDataRecordMapping,
//    metadataMapping = userDataMetadataRecordMapping
//  )
//  val userService = new UserServiceImpl[E](
//    users = userDao,
//    passwordService = passwordService
//  )
//
//  val userLoginService = new UserLoginServiceImpl[E](
//    logger = WriterLogger("userLoginService").liftS,
//    users = userService,
//    tokens = tokenService,
//    passwords = passwordService
//  )
}
