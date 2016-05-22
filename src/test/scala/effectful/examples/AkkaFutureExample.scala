package effectful.examples

import scala.language.higherKinds
import com.mchange.v2.c3p0.ComboPooledDataSource
import effectful._
import effectful.examples.adapter.akka._
import effectful.examples.effects.logging.writer.{LogWriter, WriterLogger}
import effectful.examples.effects.sql.jdbc.JdbcSqlDriver
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user.TokenService
import scala.concurrent.Future
import scala.concurrent.duration._

object AkkaFutureExample {
  import scala.concurrent.ExecutionContext.Implicits.global

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

  type E[A] = Future[LogWriter[A]]
  implicit val effectSystem_E = Nested[Future,LogWriter]

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,E](
    sql = sqlDriver.liftS,
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokenService = new TokenServiceImpl[E](
    logger = WriterLogger("tokenService").liftS,
    uuids = uuidService.liftS,
    tokens = tokenDao,
    tokenDefaultDuration = 10.days
  )


//  val delayService = new DelayService[Future] {
//    override def delay(duration: FiniteDuration): Future[Unit] =
//      // todo: use actor service or java for true non-blocking delay
//      Future { Thread.sleep(duration.toMillis)}
//  }
//  val passwordService = new PasswordServiceImpl[Future](
//    delayService = delayService,
//    passwordMismatchDelay = 5.seconds
//  )


}
