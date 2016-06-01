//package effectful.examples
//
//import scalaz.{-\/, \/, \/-}
//import scala.concurrent.duration._
//import effectful._
//import effectful.examples.effects.logging.free._
//import effectful.examples.adapter.scalaz.writer.{LogWriter, WriterLogger}
//import effectful.examples.effects.sql.free._
//import effectful.examples.pure.dao.sql.SqlDocDao
//import effectful.examples.pure.uuid.impl.JavaUUIDs
//import effectful.examples.mapping.sql._
//import effectful.examples.pure.user.impl._
//import effectful.examples.pure.user._
//import effectful.examples.pure._
//import effectful.examples.pure.uuid.UUIDs.UUID
//import effectful.free._
//
//import scala.concurrent.Future
//
//object FreeMonadExample {
//
//  type Cmd[A] = LoggerCmd[A] \/ SqlDriverCmd[A]
//  type E[A] = Free[Cmd,A]
//
//  implicit val liftCapture_Id_Free : LiftCapture[Id,E] = implicitly
//
//  // todo: how to generalize these for all nested disjunctions?
//  implicit def liftCmd_disjunction_left[Cmd1[_],Cmd2[_]] =
//    new LiftCmd[Cmd1,({ type Cmd[A] = Cmd1[A] \/ Cmd2[A]})#Cmd] {
//      def apply[AA](cmd: Cmd1[AA]) = -\/(cmd)
//    }
//
//  implicit def liftCmd_disjunction_right[Cmd1[_],Cmd2[_]] =
//    new LiftCmd[Cmd2,({ type Cmd[A] = Cmd1[A] \/ Cmd2[A]})#Cmd] {
//      def apply[AA](cmd: Cmd2[AA]) = \/-(cmd)
//    }
//
//  val uuids = new JavaUUIDs
//
//  val sqlDriver = new FreeSqlDriver
//
//  val tokenDao = new SqlDocDao[String,Tokens.TokenInfo,E](
//    sql = sqlDriver.liftService,
//    recordMapping = tokenInfoRecordMapping,
//    metadataMapping = tokenInfoMetadataRecordMapping
//  )
//
//  val tokens = new TokensImpl[E](
//    logger = new FreeLogger("tokens").liftService,
//    uuids = uuids.liftService,
//    tokens = tokenDao,
//    tokenDefaultDuration = 10.days
//  )
//
//  tokens.find("asdf")
//
//  val passwords = new PasswordsImpl[E](
//    passwordMismatchDelay = 5.seconds
//  )
//
//
//  val userDao = new SqlDocDao[UUID,UsersImpl.UserData,E](
//    sql = sqlDriver.liftService,
//    recordMapping = userDataRecordMapping,
//    metadataMapping = userDataMetadataRecordMapping
//  )
//
//  val users = new UsersImpl[E](
//    users = userDao,
//    passwords = passwords
//  )
//
//  val userLogins = new UserLoginsImpl[E](
//    logger = new FreeLogger("userLogins").liftService,
//    users = users,
//    tokens = tokens,
//    passwords = passwords
//  )
//
//  // todo: generalize interpreter for any disjunction of commands
//  val interpreter = new Interpreter[Cmd,AkkaFutureExample.E] {
//    override implicit val E = AkkaFutureExample.exec_E
//
//    val sqlInterpreter =
//      new SqlDriverCmdInterpreter[AkkaFutureExample.E](
//        sqlDriver = AkkaFutureExample.sqlDriver.liftService
//      )
//    val logInterpreter =
//      new LoggerCmdInterpreter[AkkaFutureExample.E](
//        // todo: memoize these
//        loggerName => WriterLogger(loggerName).liftService(
//          // todo: fix me - should match implicit in effectful.package
//          new LiftCapture[LogWriter,AkkaFutureExample.E] {
//            def apply[A](ea: => LogWriter[A]) = {
//              import AkkaFutureExample.executionContext
//              Future(ea)
//            }
//          },
//          implicitly
//        )
//      )
//    override def apply[A](cmd: Cmd[A]): AkkaFutureExample.E[A] =
//      cmd match {
//        case -\/(loggingCmd) => logInterpreter(loggingCmd)
//        case \/-(sqlDriverCmd) => sqlInterpreter(sqlDriverCmd)
//      }
//  }
//}
