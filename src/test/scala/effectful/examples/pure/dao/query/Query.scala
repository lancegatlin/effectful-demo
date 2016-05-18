package effectful.examples.pure.dao.query

sealed trait Query[A]

object Query {
  case class Field[A,B](
    name: String,
    extract: A => B
  )

  sealed trait Op[A] extends Query[A] {
    def apply(a: A) : Boolean
  }

  case class EqualField[A,B](
    field: Field[A,B],
    test: Field[A,B]
  ) extends Op[A]  {
    def apply(a: A) : Boolean =
      field.extract(a) == test.extract(a)
  }

  case class EqualVal[A,B](
    field: Field[A,B],
    test: B
  )  extends Op[A] {
    def apply(a: A) : Boolean =
      field.extract(a) == test
  }

  case class LessThanField[A,B](
    field: Field[A,B],
    test: Field[A,B]
  )(implicit ob:Ordering[B]) extends Op[A]  {
    def apply(a: A) : Boolean =
      ob.lt(field.extract(a),test.extract(a))
  }

  case class LessThanVal[A,B](
    field: Field[A,B],
    test: B
  )(implicit ob:Ordering[B]) extends Op[A] {
    def apply(a: A) : Boolean =
      ob.lt(field.extract(a),test)
  }

  case class GreaterThanField[A,B](
    field: Field[A,B],
    test: Field[A,B]
  )(implicit ob:Ordering[B]) extends Op[A]  {
    def apply(a: A) : Boolean =
      ob.gt(field.extract(a),test.extract(a))
  }

  case class GreaterThanVal[A,B](
    field: Field[A,B],
    test: B
  )(implicit ob:Ordering[B]) extends Op[A] {
    def apply(a: A) : Boolean =
      ob.gt(field.extract(a),test)
  }

  case class And[A](qs: List[Query[A]]) extends Query[A]
  case class Or[A](qs: List[Query[A]]) extends Query[A]
  case class Not[A](base: Query[A]) extends Query[A]

  def mkInMemoryQuery[A](q: Query[A]) : A => Boolean = {
    def loop(query: Query[A]) : A => Boolean = {
      query match {
        case op:Op[A] => op.apply
        case And(qs) =>
          val fs = qs.map(loop)

          { a:A => fs.forall(_(a)) }
        case Or(qs) =>
          val fs = qs.map(loop)

          { a:A => fs.exists(_(a)) }
        case Not(inner) => loop(inner).andThen(b => !b)
      }
    }
    loop(q)
  }
}
