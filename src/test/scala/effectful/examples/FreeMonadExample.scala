package effectful.examples

import scala.concurrent.duration._
import effectful._
import effectful.augments.{Capture, Delay, Exceptions, Par}
import cats._
import cats.arrow.FunctionK
import effectful.examples.adapter.akka.ExecFuture
import effectful.examples.effects.logging.free._
import effectful.examples.adapter.slf4j.Slf4jLogger
import effectful.examples.adapter.writer.WriterLogger
import effectful.examples.effects.sql.free._
import effectful.examples.pure.uuid.impl.JavaUUIDs
import effectful.examples.mapping.sql._
import effectful.examples.pure.user.impl._
import effectful.examples.pure.user._
import effectful.examples.pure._
import effectful.examples.pure.dao.sql.impl.SqlDocDaoImpl
import effectful.examples.pure.uuid.UUIDs.UUID
import effectful.free._
import s_mach.concurrent.ScheduledExecutionContext

import scala.concurrent.Future

object FreeMonadExample {

  type Cmd[A] = Either[LoggerCmd[A],SqlDriverCmd[A]]
  type E[A] = Free[Cmd,A]

  // todo: shouldnt need this
  implicit def capture_Free[Cmd1[_]]  =
    Capture.fromApplicative[({ type F[A] = Free[Cmd1,A]})#F]

  implicit def lift_disjunction_left[Cmd1[_],Cmd2[_]] : FunctionK[Cmd1,({ type C[A] = Either[Cmd1[A],Cmd2[A]] })#C] =
    new FunctionK[Cmd1,({ type C[A] = Either[Cmd1[A],Cmd2[A]] })#C] {
      override def apply[A](c: Cmd1[A]) = Left(c)
    }

  implicit def lift_disjunction_right[Cmd1[_],Cmd2[_]] : FunctionK[Cmd2,({ type C[A] = Either[Cmd1[A],Cmd2[A]] })#C] =
    new FunctionK[Cmd2,({ type C[A] = Either[Cmd1[A],Cmd2[A]] })#C] {
      override def apply[A](c: Cmd2[A]) = Right(c)
    }

  implicit val uuids = new JavaUUIDs

  val sqlDriver = new FreeSqlDriver

  val tokensDao = new SqlDocDaoImpl[String,Tokens.TokenInfo,E](
    sql = sqlDriver.liftService[E],
    recordMapping = tokenInfoRecordMapping,
    metadataMapping = tokenInfoMetadataRecordMapping
  )

  val tokens = new TokensImpl[E](
    uuids = uuids.liftService[E],
    tokensDao = tokensDao,
    tokenDefaultDuration = 10.days,
    logger = FreeLogger("tokens").liftService[E]
  )

  val passwords = new PasswordsImpl[E](
    passwordMismatchDelay = 5.seconds,
    logger = FreeLogger("passwords").liftService[E]
  )


  val userDao = new SqlDocDaoImpl[UUID,UsersImpl.UserData,E](
    sql = sqlDriver.liftService[E],
    recordMapping = userDataRecordMapping,
    metadataMapping = userDataMetadataRecordMapping
  )

  val users = new UsersImpl[E](
    usersDao = userDao,
    passwords = passwords,
    logger = FreeLogger("users").liftService[E]
  )

  val userLogins = new UserLoginsImpl[E](
    users = users,
    tokens = tokens,
    passwords = passwords,
    logger = FreeLogger("userLogins").liftService[E]
  )

  // todo: generalize interpreter for any disjunction of commands
  val futInterpreter = new Interpreter[Cmd,FutureLogWriterExample.E] {
    type EE[A] = FutureLogWriterExample.E[A]
    import cats.implicits._

    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit val scheduledExecutionContext = ScheduledExecutionContext(4)
    implicit val exec_Future = ExecFuture.bindContext()(
      executionContext,
      scheduledExecutionContext,
      implicitly[cats.Monad[Future]]
    )

    // todo: clean this up
    override val C = implicitly[Capture[EE]]
    override val D = implicitly[Delay[EE]]
    override val M = implicitly[Monad[EE]]
    override val P = implicitly[Par[EE]]
    override val X = implicitly[Exceptions[EE]]

    val sqlInterpreter =
      new SqlDriverCmdInterpreter[EE](
        sqlDriver = FutureLogWriterExample.sqlDriver.liftService
      )
    val logInterpreter =
      new LoggerCmdInterpreter[EE](
        // todo: memoize these
        loggerName => WriterLogger(loggerName).liftService
      )
    override def apply[A](cmd: Cmd[A]): EE[A] =
      cmd match {
        case Left(loggingCmd) => logInterpreter(loggingCmd)
        case Right(sqlDriverCmd) => sqlInterpreter(sqlDriverCmd)
      }
  }

  val idInterpreter = new Interpreter[Cmd,Id] {
    type EE[A] = Id[A]
    // todo: clean this up
    override val C = implicitly[Capture[EE]]
    override val D = implicitly[Delay[EE]]
    override val M = implicitly[Monad[EE]]
    override val P = implicitly[Par[EE]]
    override val X = implicitly[Exceptions[EE]]

    val sqlInterpreter =
      new SqlDriverCmdInterpreter[EE](
        sqlDriver = IdExample.sqlDriver
      )
    val logInterpreter =
      new LoggerCmdInterpreter[EE](
        // todo: memoize these
        loggerName => Slf4jLogger(loggerName)
      )
    override def apply[A](cmd: Cmd[A]): EE[A] =
      cmd match {
        case Left(loggingCmd) => logInterpreter(loggingCmd)
        case Right(sqlDriverCmd) => sqlInterpreter(sqlDriverCmd)
      }
  }
}
