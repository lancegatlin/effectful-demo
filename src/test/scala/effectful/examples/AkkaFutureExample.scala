package effectful.examples

import com.mchange.v2.c3p0.ComboPooledDataSource
import effectful._
import effectful.examples.adapter.akka._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.scalaz.writer._
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.UUIDService.UUID
import effectful.examples.pure.user._
import effectful.examples.pure._
import s_mach.concurrent.ScheduledExecutionContext

import scala.concurrent._
import scala.concurrent.duration._

object AkkaFutureExample {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val scheduledExecutionContext = ScheduledExecutionContext(4)

  type E[A] = Future[LogWriter[A]]

  implicit val exec_Future = ExecFuture()
  implicit val exec_E = CompositeExec[Future,LogWriter]

  // todo: generalize this
  implicit val liftCapture_Writer_Future = new LiftCapture[LogWriter,E] {
    override def apply[A](
      ea: => LogWriter[A]
    ): E[A] =
      Future.successful(ea)
  }

  val uuidService = new JavaUUIDService

  val pool = new ComboPooledDataSource()
  pool.setDriverClass("org.postgresql.Driver")
  pool.setJdbcUrl("jdbc:postgresql://localhost/testdb")
  pool.setUser("test")
  pool.setPassword("test password")
  pool.setMinPoolSize(5)
  pool.setAcquireIncrement(5)
  pool.setMaxPoolSize(20)
  // todo: use in-memory h2
  // todo: generate schema
  // todo: initialize with schema

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = pool.getConnection,
    uuids = uuidService
  )

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,E](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokenService = new TokenServiceImpl[E](
    logger = WriterLogger("tokenService").liftService,
    uuids = uuidService.liftService,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

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
    logger = WriterLogger("userLoginService").liftService,
    users = userService,
    tokens = tokenService,
    passwords = passwordService
  )
}
