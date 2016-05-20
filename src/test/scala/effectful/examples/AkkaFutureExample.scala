package effectful.examples

import scala.language.higherKinds
import com.mchange.v2.c3p0.ComboPooledDataSource
import effectful._
import effectful.examples.adapter.akka._
import effectful.examples.effects.delay.DelayService
import effectful.examples.effects.logging.writer.{LogWriter, WriterLogger}
import effectful.examples.effects.sql.jdbc.JdbcSqlDriver
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.impl.JavaUUIDService
import effectful.examples.pure.user.impl.{PasswordServiceImpl, TokenServiceImpl}
import effectful.examples.mapping.sql._
import effectful.examples.pure.user.TokenService

import scala.collection.generic.CanBuildFrom
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

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = pool.getConnection,
    uuids = uuidService
  )

  type E[A] = Future[LogWriter[A]]
  implicit object E extends EffectSystem[E] {
    override def map[A, B](m: E[A], f: (A) => B): E[B] =
      m.map(_.map(f))
    override def flatMap[A, B](m: E[A], f: (A) => E[B]): E[B] =
      ???

    override def Try[A](_try: =>E[A])(_catch: PartialFunction[Throwable, E[A]]): E[A] =
      _try.recoverWith(_catch)

    override def widen[A, AA >: A](ea: E[A]): E[AA] =
      ea.asInstanceOf[E[AA]]

    override def sequence[F[AA] <: Traversable[AA], A](fea: F[E[A]])(implicit cbf: CanBuildFrom[Nothing, A, F[A]]): E[F[A]] = {
      import scalaz._,Scalaz._
      
    }

    override def apply[A](a: => A): E[A] =
      Future(LogWriter(a))
  }

  val tokenDao = new SqlDocDao[String,TokenService.TokenInfo,E](
    sql = sqlDriver.liftS[E],
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokenService = new TokenServiceImpl[E](
    logger = WriterLogger("tokenService").liftS[E],
    uuids = uuidService.liftS[E],
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
