package effectful.examples

import effectful._
import effectful.aspects.{Delay, Exceptions}
import effectful.cats._
import effectful.examples.adapter.jdbc.JdbcSqlDriver
import effectful.examples.adapter.scalaz.writer._
import effectful.examples.pure.dao.sql.SqlDocDao
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.pure.user.impl._
import effectful.examples.mapping.sql._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.uuid.UUIDs.UUID
import effectful.impl.StdPar
import s_mach.concurrent.ScheduledExecutionContext

import scala.concurrent._
import scala.concurrent.duration._

object FutureLogWriterExample {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val scheduledExecutionContext = ScheduledExecutionContext(4)

  type E[A] = Future[LogWriter[A]]

  implicit val monad_Future = new Monad[Future] {
    // Note: these bind ExecutionContext above
    def map[A, B](m: Future[A])(f: (A) => B) = m.map(f)
    def flatMap[A, B](m: Future[A])(f: (A) => Future[B]) = m.flatMap(f)
    def widen[A, AA >: A](ea: Future[A]) = ea
    def pure[A](a: A) = Future.successful(a)
  }
  implicit val flatSequence_Future_LogWriter = new FlatSequence[Future,LogWriter] {
    val F:Monad[Future] = implicitly
    def flatSequence[A, B](ga: LogWriter[A])(f: (A) => Future[LogWriter[B]]) : Future[LogWriter[B]] = {
      import scalaz.std.list._
      val (acc,a) = ga.run
      F.map(f(a))(_.<++:(acc))
    }
  }
  implicit val monad_LogWriter = new Monad[LogWriter] {
    def map[A, B](m: LogWriter[A])(f: (A) => B) = m.map(f)
    def flatMap[A, B](m: LogWriter[A])(f: (A) => LogWriter[B]) = {
      import scalaz.std.list._
      m.flatMap(f)
    }
    def widen[A, AA >: A](ea: LogWriter[A]) = ea.asInstanceOf[LogWriter[AA]]
    def pure[A](a: A) = LogWriter(a)
  }
  implicit val monad_E = new Monad[E] { // composite
    val F:Monad[Future] = implicitly
    val G:Monad[LogWriter] = implicitly
    val FG:FlatSequence[Future,LogWriter] = implicitly

    def map[A, B](m: E[A])(f: (A) => B) =
      F.map(m)(g => G.map(g)(f))
    def flatMap[A, B](m: E[A])(f: (A) => E[B]) =
      F.flatMap(m)(g => FG.flatSequence(g)(f))
    def widen[A, AA >: A](ea: E[A]) = F.map(ea)(G.widen)
    def pure[A](a: A) = F.pure(G.pure(a))
  }

  implicit val capture_Future = new Capture[Future] {
    // Note: this binds ExecutionContext above
    def capture[A](a: => A) = Future(a)
  }
  implicit val capture_LogWriter = new Capture[LogWriter] { // conversion
    val E:Applicative[LogWriter] = implicitly
    def capture[A](a: => A) = E.pure(a)
  }
  implicit val capture_E = new Capture[E] { // composite
    val F:Capture[Future] = implicitly
    val G:Capture[LogWriter] = implicitly
    def capture[A](a: => A) = F.capture(G.capture(a))
  }

  implicit val captureTransform_Id_E = new CaptureTransform[Id,E] { // conversion
    val E:Applicative[E] = implicitly
    def apply[A](f: => Id[A]) = E.pure(f)
  }
  implicit val captureTransform_LogWriter_E = new CaptureTransform[LogWriter,E] {
    val F:Capture[Future] = implicitly
    def apply[A](f: => LogWriter[A]) = F.capture(f)
  }

  implicit val par_E = StdPar[E]
  implicit val exceptions_E = new Exceptions[E] {
    def attempt[A](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]]) =
      ??? //_try.recover(_catch)
    def attemptFinally[A, U](_try: => E[A])(_catch: PartialFunction[Throwable, E[A]])(_finally: => E[U]) =
      ??? //_try.recover(_catch)
    def failure(t: Throwable) = ???
    def success[A](a: A) = ???
  }
  implicit val delay_E = new Delay[E] {
    def delay(duration: FiniteDuration) = ???
  }

  implicit val uuids = new JavaUUIDs

  val sqlDriver = new JdbcSqlDriver(
    getConnectionFromPool = SqlDb.pool.getConnection,
    uuids = uuids
  )

  val tokensDao = new SqlDocDao[String,Tokens.TokenInfo,E](
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
    passwordMismatchDelay = 5.seconds
  )

  val userDao = new SqlDocDao[UUID,UsersImpl.UserData,E](
    sql = sqlDriver.liftService[E],
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )
  val users = new UsersImpl[E](
    usersDao = userDao,
    passwords = passwords
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
