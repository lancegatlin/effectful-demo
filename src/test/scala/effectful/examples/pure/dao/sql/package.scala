package effectful.examples.pure.dao

import scala.language.implicitConversions
import effectful.examples.effects.sql._

package object sql {
  type SqlString = String with SqlStringTag
  def SqlString(s: String) = s.asInstanceOf[SqlString]

  implicit val sqlPrint_SqlString = new PrintSql[SqlString] {
    override def printSql(a: SqlString): SqlString = a
  }
  
  type ColName = String with ColNameTag
  implicit def ColName(s: String) : ColName = s.asInstanceOf[ColName]
  implicit val splPrint_ColName = PrintSql.backtick[ColName](_.sql)
  
  type TableName = String with TableNameTag
  implicit def TableName(s: String) : TableName = s.asInstanceOf[TableName]
  implicit val splPrint_TableName = PrintSql.backtick[TableName](_.sql)

  val `?` = "?".sql
  def `repeat_?`(n: Int) : IndexedSeq[SqlString] =
    (0 until n).map(_ => `?`)

  implicit class SqlStringPML(val self: SqlString) extends AnyVal {
    def *(n: Int) =
      self.asInstanceOf[String] * n
  }

  implicit class EverythingPML[A](val self: A) extends AnyVal {
    def toSqlVal(implicit sqlValFormat: SqlValFormat[A]) : SqlVal =
      sqlValFormat.toSqlVal(self)
    def printSql(implicit sqlPrint:PrintSql[A]) : SqlString =
      sqlPrint.printSql(self)
    def toCharData(implicit fmt: CharDataFormat[A]) : CharData =
      fmt.toCharData(self)
    def toBinData(implicit fmt: BinDataFormat[A]) : BinData =
      fmt.toBinData(self)
  }

  implicit class TraversablePML[A](val self: Traversable[A]) extends AnyVal {
    def mkSqlString(sep: String)(implicit sqlPrint: PrintSql[A]) : SqlString =
      self.map(_.printSql).mkString(sep).sql
    def mkSqlString(implicit sqlPrint: PrintSql[A]) : SqlString =
      self.map(_.printSql).mkString.sql
  }

  implicit class StringPML(val self: String) extends AnyVal {
    def sql = SqlString(self)
  }

  implicit class StringContextPML(val self: StringContext) extends AnyVal {
    def sql(args: SqlString*) : SqlString = SqlString(self.s(args:_*))
  }

  implicit class SqlValPML(val self: SqlVal) extends AnyVal {
    def as[S <: SqlVal] : S =
      self.asInstanceOf[S]

    def asNullable[S <: SqlVal] : Option[S] =
      self match {
        case SqlVal.NULL(_) => None
        case _ => Some(self.as[S])
      }
  }

  implicit class OptionSqlValPML(val self: Option[SqlVal]) extends AnyVal {
    def orSqlNull(sqlType: SqlType) : SqlVal =
      self match {
        case Some(v) => v
          // todo: either this to OptionSqlXXXPML to preserve sql type (sql type required by JDBC PreparedStatement.setNull
        case None => SqlVal.NULL(sqlType)
      }
  }

  implicit def everythingToSqlString[A](a: A)(implicit sqlPrint: PrintSql[A]) : SqlString =
    sqlPrint.printSql(a)

  implicit class CharDataPML(val self: CharData) extends AnyVal {
    def to[A](implicit fmt: CharDataFormat[A]) : A =
      fmt.fromCharData(self)
  }

  implicit class BinDataPML(val self: BinData) extends AnyVal {
    def to[A](implicit fmt: BinDataFormat[A]) : A =
      fmt.fromBinData(self)
  }

}
