package effectful.examples

import effectful._
import effectful.examples.adapter.akka._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.scalaz.writer._
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.uuid.UUIDs
import effectful.examples.pure.uuid.UUIDs.UUID
import s_mach.concurrent.ScheduledExecutionContext

import scala.concurrent._
import scala.concurrent.duration._

object AkkaFutureExample {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val scheduledExecutionContext = ScheduledExecutionContext(4)

  type E[A] = Future[LogWriter[A]]

  implicit val exec_Future = ExecFuture()
  implicit val exec_E = CompositeExec[Future,LogWriter]

  val uuids = new JavaUUIDs

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuids
  )

  val tokenDao = new SqlDocDao[String,Tokens.TokenInfo,E](
    sql = sqlDriver.liftService,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokens = new TokensImpl[E](
    logger = WriterLogger("tokens").liftService,
    uuids = uuids.liftService,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

  val passwords = new PasswordsImpl[E](
    passwordMismatchDelay = 5.seconds
  )

  val userDao = new SqlDocDao[UUID,UsersImpl.UserData,E](
    sql = sqlDriver.liftService,
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val users = new UsersImpl[E](
    users = userDao,
    passwords = passwords
  )

  val userLogins = new UserLoginsImpl[E](
    logger = WriterLogger("userLogins").liftService,
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
