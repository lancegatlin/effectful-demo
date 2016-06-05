package effectful.examples

import effectful._
import effectful.aspects._
import effectful.cats._
import effectful.examples.adapter.akka._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.scalaz.writer._
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
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

  val temp3 = implicitly[Monad[Future]]
  val temp4 = implicitly[Monad[LogWriter]]
  val temp5 = implicitly[FlatSequence[Future,LogWriter]]

//  val temp8 = implicitly[CompositePar[Future,LogWriter]]
//  val temp7 = implicitly[CompositeExceptions[Future,LogWriter]]
//  val temp9 = implicitly[CompositeDelay[Future,LogWriter]]

  val temp10 = implicitly[Par[E]]
  val temp11 = implicitly[Exceptions[E]]
  val temp12 = implicitly[Delay[E]]

  val temp18 = implicitly[Monad[Future]]
  val temp19 = implicitly[Monad[LogWriter]]
  val temp20 = implicitly[FlatSequence[Future,LogWriter]]
  implicit val temp6 = implicitly[CompositeMonad[Future,LogWriter]]
  val temp2 = implicitly[Monad[E]]

  val temp14 = implicitly[Capture[Future]]
  val temp15 = implicitly[Capture[LogWriter]]
  implicit val temp16 = implicitly[CompositeCapture[Future,LogWriter]]
  val temp = implicitly[Capture[E]]

  val temp17 = implicitly[NaturalTransformation[Id,E]]

  val temp21 = implicitly[Applicative[E]]
  val temp1 = implicitly[CaptureTransform[Id,E]]


//  val tokensDao = new SqlDocDao[String,Tokens.TokenInfo,E](
//    sql = sqlDriver.liftService[E],
//    recordMapping = tokenInfoRecordMapping,
//    metadataMapping = tokenInfoMetadataRecordMapping
//  )

//  val tokens = new TokensImpl[E](
//    logger = WriterLogger("tokens").liftService[E],
//    uuids = uuids.liftService[E],
//    tokensDao = tokensDao,
//    tokenDefaultDuration = 10.days
//  )
//
//  val passwords = new PasswordsImpl[E](
//    passwordMismatchDelay = 5.seconds
//  )
//
//  val userDao = new SqlDocDao[UUID,UsersImpl.UserData,E](
//    sql = sqlDriver.liftService[E],
//    recordMapping = userDataRecordMapping,
//    metadataMapping = userDataMetadataRecordMapping
//  )
//  val users = new UsersImpl[E](
//    usersDao = userDao,
//    passwords = passwords
//  )
//
//  val userLogins = new UserLoginsImpl[E](
//    logger = WriterLogger("userLogins").liftService[E],
//    users = users,
//    tokens = tokens,
//    passwords = passwords
//  )
//  /*
//  import scala.concurrent._
//  import scala.concurrent.duration._
//  import scala.concurrent.ExecutionContext.Implicits.global
//  import effectful.examples.adapter.scalaz.writer.LogWriter
//  def get[A](f: Future[LogWriter[A]]) = {
//    val (log,result) = Await.result(f,Duration.Inf).run
//    log.foreach(println)
//    result
//  }
//   */
}
