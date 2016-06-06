package effectful.examples.pure.dao.sql

trait SqlPrint[A] {
  def print(a: A) : SqlString
}

object SqlPrint {
  def apply[A](f: A => SqlString) : SqlPrint[A] = new SqlPrint[A] {
    override def print(a: A): SqlString = f(a)
  }

  def quoted[A](f: A => SqlString) : SqlPrint[A] = new SqlPrint[A] {
    override def print(a: A): SqlString =
      s"'${f(a)}'".sql
  }

  implicit val sqlPrint_Boolean = SqlPrint[Boolean](if(_) "true".sql else "false".sql)
  implicit val sqlPrint_Char = SqlPrint.quoted[Char](_.toString.sql)
  implicit val sqlPrint_String = SqlPrint.quoted[String](_.sql)
  implicit val sqlPrint_Byte = SqlPrint[Byte](_.toString.sql)
  implicit val sqlPrint_Short = SqlPrint[Short](_.toString.sql)
  implicit val sqlPrint_Int = SqlPrint[Int](_.toString.sql)
  implicit val sqlPrint_Long = SqlPrint[Long](_.toString.sql)
  implicit val sqlPrint_Float = SqlPrint[Float](_.toString.sql)
  implicit val sqlPrint_Double = SqlPrint[Double](_.toString.sql)
  implicit val sqlPrint_BigInt = SqlPrint[BigInt](_.toString.sql)
  implicit val sqlPrint_BigDecimal = SqlPrint[BigDecimal](_.toString.sql)
}

case class ColName(name: String)
object ColName {
  implicit val splPrint_Col = SqlPrint.quoted[ColName](_.name.sql)
}

case class TableName(name: String)
object TableName {
  implicit val splPrint_Table = SqlPrint.quoted[TableName](_.name.sql)
}

case class DbName(name: String)
object DbName {
  implicit val splPrint_Db = SqlPrint.quoted[DbName](_.name.sql)
}