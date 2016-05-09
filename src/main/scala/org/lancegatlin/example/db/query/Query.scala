package org.lancegatlin.example.db.query

sealed trait Query[A]

object Query {
  case class Field[A,B](
    name: String,
    extract: A => B
  )
  
  sealed trait Op
  object Op {
    case object LessThan extends Op
    case object LessThanEquals extends Op
    case object GreaterThan extends Op
    case object GreaterThanEquals extends Op
    case object Equals extends Op
    case object NotEquals extends Op
  }

  case class Test[A,B](
    field: Field[A,B],
    test: Either[Field[A,B],B],
    op: Op
  ) extends Query[A]
  case class And[A](qs: List[Query[A]]) extends Query[A]
  case class Or[A](qs: List[Query[A]]) extends Query[A]
  case class Not[A](base: Query[A])

}
