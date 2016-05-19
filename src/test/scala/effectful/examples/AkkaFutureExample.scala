package effectful.examples

import effectful._
import effectful.examples.adapter.akka._
import effectful.examples.effects.delay.DelayService
import effectful.examples.effects.logging.writer.WriterLogger
import effectful.examples.effects.sql.SqlDriver
import effectful.examples.effects.sql.jdbc.JdbcSqlDriver
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.TokenService
import effectful.examples.pure.user.impl.{PasswordServiceImpl, TokenServiceImpl}

import scala.concurrent.Future
import scala.concurrent.duration._

object AkkaFutureExample {
  import scala.concurrent.ExecutionContext.Implicits.global

  val delayService = new DelayService[Future] {
    override def delay(duration: FiniteDuration): Future[Unit] =
      Future { Thread.sleep(duration.toMillis)}
  }
  val passwordService = new PasswordServiceImpl[Future](
    delayService = delayService,
    passwordMismatchDelay = 5.seconds
  )

  val uuidService = new JavaUUIDService

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = { cinfo =>
      ???
    },
    uuids = uuidService
  )
  val tokenDao = new SqlDocDao(
    sql = sqlDriver,
    tableName = "tokens",
    metadataTableName = "tokens"
  )
  val tokenService = new TokenServiceImpl[Future](
    logger = new WriterLogger("tokenService").liftS,
    uuids = uuidService.liftS,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )

}
