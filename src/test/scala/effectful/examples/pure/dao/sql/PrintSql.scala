package effectful.examples.pure.dao.sql

trait PrintSql[A] {
  def printSql(a: A) : SqlString
}

object PrintSql {
  def apply[A](f: A => SqlString) : PrintSql[A] = new PrintSql[A] {
    override def printSql(a: A): SqlString = f(a)
  }

  def quoted[A](f: A => SqlString) : PrintSql[A] = new PrintSql[A] {
    override def printSql(a: A): SqlString =
      s"'${f(a)}'".sql
  }

  def backtick[A](f: A => SqlString) : PrintSql[A] = new PrintSql[A] {
    override def printSql(a: A): SqlString =
      s"`${f(a)}`".sql
  }

  implicit val sqlPrint_Boolean = PrintSql[Boolean](if(_) "true".sql else "false".sql)
  implicit val sqlPrint_Char = PrintSql.quoted[Char](_.toString.sql)
  implicit val sqlPrint_String = PrintSql.quoted[String](_.sql)
  implicit val sqlPrint_Byte = PrintSql[Byte](_.toString.sql)
  implicit val sqlPrint_Short = PrintSql[Short](_.toString.sql)
  implicit val sqlPrint_Int = PrintSql[Int](_.toString.sql)
  implicit val sqlPrint_Long = PrintSql[Long](_.toString.sql)
  implicit val sqlPrint_Float = PrintSql[Float](_.toString.sql)
  implicit val sqlPrint_Double = PrintSql[Double](_.toString.sql)
  implicit val sqlPrint_BigInt = PrintSql[BigInt](_.toString.sql)
  implicit val sqlPrint_BigDecimal = PrintSql[BigDecimal](_.toString.sql)

}