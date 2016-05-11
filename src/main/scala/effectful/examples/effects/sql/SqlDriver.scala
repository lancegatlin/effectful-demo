package effectful.examples.effects.sql

import scala.language.higherKinds

trait SqlDriver[E[_]] {
  def getConnection(url: String, username: String, password: String) : E[Connection]

  def prepare(statement: String)(implicit connection: Connection) : E[PreparedStatement]
  def executePreparedQuery(
    preparedStatement: PreparedStatement
  )(
    batches: Seq[SqlVal]*
  )(implicit
    connection: Connection
  ) : E[Cursor]

  def executePreparedUpdate(
    preparedStatement: PreparedStatement
  )(
    batches: Seq[SqlVal]*
  )(implicit
    connection: Connection
  ) : E[Int]

  def executeQuery(statement: String)(implicit connection: Connection) : E[Cursor]
  def executeUpdate(statement: String)(implicit connection: Connection) : E[Int]
}