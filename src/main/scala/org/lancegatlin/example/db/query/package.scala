package org.lancegatlin.example.db

package object query {
  import Query._

  implicit class QueryFieldPML[A,B](val self:Field[A,B]) extends AnyVal {
    def ===(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.Equals)
    def ===(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.Equals)
    def !==(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.NotEquals)
    def !==(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.NotEquals)
    def <(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.LessThan)
    def <(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.LessThan)
    def <=(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.LessThanEquals)
    def <=(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.LessThanEquals)
    def >(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.GreaterThan)
    def >(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.GreaterThan)
    def >=(other: Field[A,B]) : Query.Test[A,B] =
      Query.Test(self,Left(other),Op.GreaterThanEquals)
    def >=(value: B) : Query.Test[A,B] =
      Query.Test(self,Right(value),Op.GreaterThanEquals)
  }

  implicit class QueryTestPML[A](val self: Query.Test[A,_]) extends AnyVal {
    def and(other: Query[A]) : Query.And[A] = Query.And(List(self,other))
    def or(other: Query[A]) : Query.Or[A] = Query.Or(List(self,other))
    def unary_!(other: Query[A]) : Query.Not[A] = Query.Not(self)
  }

  implicit class QueryAndPML[A](val self: Query.And[A]) extends AnyVal {
    def and(other: Query.Test[A,_]) : Query.And[A] = Query.And(other :: self.qs)
    def and(other: Query.Or[A]) : Query.And[A] = Query.And[A](other :: self.qs)
    def and(other: Query.And[A]) : Query.And[A] = Query.And[A](self.qs ::: other.qs)
    def or(other: Query[A]) : Query.Or[A] = Query.Or[A](List(self,other))
    def unary_! : Query.Not[A] = Query.Not(self)
  }

  implicit class QueryOrPML[A](val self: Query.Or[A]) extends AnyVal {
    def or(other: Query.Test[A,_]) : Query.Or[A] = Query.Or[A](other :: self.qs)
    def or(other: Query.And[A]) : Query.Or[A] = Query.Or[A](other :: self.qs)
    def or(other: Query.Or[A]) : Query.Or[A] = Query.Or[A](self.qs ::: other.qs)
    def and(other: Query[A]) : Query.And[A] = Query.And[A](List(self,other))
    def unary_! : Query.Not[A] = Query.Not(self)
  }

}
