package effectful.examples

import scalaz.{-\/, \/, \/-}
import scala.concurrent.duration._
import effectful._
import effectful.examples.effects.logging.free._
import effectful.examples.effects.sql.free._
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.mapping.sql._
import effectful.examples.pure.UUIDService.UUID
import effectful.examples.pure.user.impl._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.free._

object FreeMonadExample {

  type Cmd[A] = LoggingCmd[A] \/ SqlDriverCmd[A]
  type E[A] = Free[Cmd,A]

  // todo: generalize these
  implicit val liftCmd_LoggingCmd_Cmd = new LiftCmd[LoggingCmd,Cmd] {
    override def apply[AA](cmd: LoggingCmd[AA]): Cmd[AA] =
      -\/(cmd)
  }

  implicit val liftCmd_FreeLoggingCmd_Cmd = new LiftCmd[SqlDriverCmd,Cmd] {
    override def apply[AA](cmd: SqlDriverCmd[AA]): Cmd[AA] =
      \/-(cmd)
  }

  val uuidService = new JavaUUIDService

  val sqlDriver = new FreeSqlDriver

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,E](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokenService = new TokenServiceImpl[E](
    logger = new FreeLogger("tokenService").liftService,
    uuids = uuidService.liftService,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

  tokenService.find("asdf")

  val passwordService = new PasswordServiceImpl[E](
    passwordMismatchDelay = 5.seconds
  )


  val userDao = new SqlDocDao[UUID,UserServiceImpl.UserData,E](
    sql = sqlDriver.liftService,
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )

  val userService = new UserServiceImpl[E](
    users = userDao,
    passwordService = passwordService
  )

  val userLoginService = new UserLoginServiceImpl[E](
    logger = new FreeLogger("userLoginService").liftService,
    users = userService,
    tokens = tokenService,
    passwords = passwordService
  )

//  val i = new Interpreter[Cmd,AkkaFutureExample.E] {
//    override implicit val E = AkkaFutureExample.E
//    val sqlI = new SqlDriverCmdInterpreter[AkkaFutureExample.E](
//      AkkaFutureExample.sqlDriver.liftService
//    )
//    val logI = new LoggerCmdInterpreter[AkkaFutureExampl.E](
//
//    )
//    override def apply[A](cmd: Cmd[A]): E[A] =
//      cmd match {
//        case -\/(loggingCmd) =>
//        case \/-(sqlDriverCmd) => sqlI(sqlDriverCmd)
//      }
//  }
}
