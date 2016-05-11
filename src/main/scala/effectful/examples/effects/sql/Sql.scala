package effectful.examples.effects.sql

import scala.language.higherKinds

trait Sql[E[_]] {
  def getConnection(url: String, username: String, password: String) : E[Connection]

  def prepare(connection: Connection, statement: String) : E[PreparedStatement]
  def executePreparedQuery(preparedStatement: PreparedStatement, batches: Seq[SqlVal]*) : E[Cursor]
  def executePreparedUpdate(preparedStatement: PreparedStatement, batches: Seq[SqlVal]*) : E[Int]

  def executeQuery(connection: Connection, statement: String) : E[Cursor]
  def executeUpdate(connection: Connection, statement: String) : E[Int]
}