package effectful.examples.effects.db

package object query {
  import Query._

  implicit class QueryOpPML[A](val self: Query.Op[A]) extends AnyVal {
    def and(other: Query[A]) = Query.And(List(self,other))
    def or(other: Query[A]) = Query.Or(List(self,other))
    def unary_! = Query.Not(self)
  }

  implicit class QueryAndPML[A](val self: Query.And[A]) extends AnyVal {
    def and(other: Query.Op[A]) = Query.And(other :: self.qs)
    def and(other: Query.Or[A]) = Query.And(other :: self.qs)
    def and(other: Query.And[A]) = Query.And(self.qs ::: other.qs)
    def or(other: Query[A]) = Query.Or(List(self,other))
    def unary_! = Query.Not(self)
  }

  implicit class QueryOrPML[A](val self: Query.Or[A]) extends AnyVal {
    def or(other: Query.Op[A]) = Query.Or(other :: self.qs)
    def or(other: Query.And[A]) = Query.Or(other :: self.qs)
    def or(other: Query.Or[A]) = Query.Or(self.qs ::: other.qs)
    def and(other: Query[A]) = Query.And(List(self,other))
    def unary_! = Query.Not(self)
  }

  implicit class QueryFieldPML[A,B](val self:Field[A,B]) extends AnyVal {
    def ===(other: Field[A,B]) =
      Query.EqualField(self,other)
    def ===(value: B) =
      Query.EqualVal(self,value)
    def !==(other: Field[A,B]) =
      !(self === other)
    def !==(value: B) =
      !(self === value)
    def <(other: Field[A,B])(implicit ob:Ordering[B]) =
      Query.LessThanField(self,other)
    def <(value: B)(implicit ob:Ordering[B]) =
      Query.LessThanVal(self,value)
    def <=(other: Field[A,B])(implicit ob:Ordering[B]) =
      !(self > other)
    def <=(value: B)(implicit ob:Ordering[B]) =
      !(self > value)
    def >(other: Field[A,B])(implicit ob:Ordering[B]) =
      Query.GreaterThanField(self,other)
    def >(value: B)(implicit ob:Ordering[B]) =
      Query.GreaterThanVal(self,value)
    def >=(other: Field[A,B])(implicit ob:Ordering[B]) =
      !(self < other)
    def >=(value: B)(implicit ob:Ordering[B]) =
      !(self < value)
  }

}
