package effectful.examples

import effectful._
import effectful.cats._
import effectful.examples.adapter.akka._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.scalaz.writer._
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.dao.sql.impl.SqlDocDaoImpl
import effectful.examples.pure.uuid.UUIDs.UUID
import s_mach.concurrent.ScheduledExecutionContext

import scala.concurrent._
import scala.concurrent.duration._

object FutureLogWriterExample {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val scheduledExecutionContext = ScheduledExecutionContext(4)

  type E[A] = Future[LogWriter[A]]

  implicit val exec_Future = ExecFuture.bindContext()(
    executionContext,
    scheduledExecutionContext
  )

  // todo: shouldnt need this
  implicit val capture_LogWriter = Capture.fromApplicative[LogWriter]

  implicit val uuids = new JavaUUIDs

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuids
  )

  val tokensDao = new SqlDocDaoImpl[String,Tokens.TokenInfo,E](
    sql = sqlDriver.liftService[E],
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokens = new TokensImpl[E](
    logger = WriterLogger("tokens").liftService[E],
    uuids = uuids.liftService[E],
    tokensDao = tokensDao,
    tokenDefaultDuration = 10.days
  )

  val passwords = new PasswordsImpl[E](
    passwordMismatchDelay = 5.seconds,
    logger = WriterLogger("passwords").liftService[E]
  )

  val userDao = new SqlDocDaoImpl[UUID,UsersImpl.UserData,E](
    sql = sqlDriver.liftService[E],
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val users = new UsersImpl[E](
    usersDao = userDao,
    passwords = passwords,
    logger = WriterLogger("users").liftService[E]
  )

  val userLogins = new UserLoginsImpl[E](
    logger = WriterLogger("userLogins").liftService[E],
    users = users,
    tokens = tokens,
    passwords = passwords
  )
  /*
  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import effectful.examples.adapter.scalaz.writer.LogWriter
  def get[A](f: Future[LogWriter[A]]) = {
    val (log,result) = Await.result(f,Duration.Inf).run
    log.foreach(println)
    result
  }
   */
}
