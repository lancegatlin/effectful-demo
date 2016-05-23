package effectful.examples

import effectful._
import effectful.examples.effects.logging.free.{FreeLogger, FreeLoggingCmd, LoggingCmd}
import effectful.examples.effects.par.impl.FakeParSystem
import effectful.examples.effects.sql.free.{FreeSqlDriver, FreeSqlDriverCmd, SqlDriverCmd}
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.TokenService
import effectful.examples.mapping.sql._
import effectful.examples.pure.user.impl.TokenServiceImpl

import scalaz.\/
import scala.concurrent.duration._


object FreeMonadExample {

  type Cmd[A] = LoggingCmd[A] \/ SqlDriverCmd[A]
  type E[A] = Free[Cmd,A]

  implicit val fakeParSystem = new FakeParSystem[E]

  implicit val liftE_FreeLogger_E = new LiftE[FreeLoggingCmd,E] {
    def apply[A](
      ea: => FreeLoggingCmd[A]
    )(implicit
      E: EffectSystem[FreeLoggingCmd],
      F: EffectSystem[E]
    ) = ??? // todo: how?
  }

  implicit val liftE_SqlDriver_E = new LiftE[FreeSqlDriverCmd,E] {
    def apply[A](
      ea: => FreeSqlDriverCmd[A]
    )(implicit
      E: EffectSystem[FreeSqlDriverCmd],
      F: EffectSystem[E]
    ) = ??? // todo: how?
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
