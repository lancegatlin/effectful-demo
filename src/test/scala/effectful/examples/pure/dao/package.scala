package effectful.examples.pure

import effectful._
import effectful.cats.CaptureTransform
import effectful.examples.pure.dao.query.Query

package object dao {
  implicit def liftS_DocDao[ID,A] = {
    type S[G[_]] = DocDao[ID,A,G]

    new LiftService[S] {
      override def apply[F[_],G[_]](
        s: DocDao[ID,A,F]
      )(implicit
        X: CaptureTransform[F,G]
      ) = {
        import DocDao._
        new DocDao[ID,A,G] {
          override def exists(id: ID) =
            X(s.exists(id))
          override def update(id: ID, value: A) =
            X(s.update(id,value))
          override def batchRemove(ids: Traversable[ID]) =
            X(s.batchRemove(ids))
          override def insert(id: ID, a: A) =
            X(s.insert(id,a))
          override def findById(id: ID) =
            X(s.findById(id))
          override def batchUpsert(records: Traversable[(ID, A)]) =
            X(s.batchUpsert(records))
          override def findAll(start: Int, batchSize: Int) =
            X(s.findAll(start,batchSize))
          override def batchExists(ids: Traversable[ID]) =
            X(s.batchExists(ids))
          override def remove(id: ID) =
            X(s.remove(id))
          override def batchUpdate(records: Traversable[(ID, A)]) =
            X(s.batchUpdate(records))
          override def batchInsert(records: Traversable[(ID, A)]) =
            X(s.batchInsert(records))
          override def find(query: Query[A]) =
            X(s.find(query))
          override def upsert(id: ID, a: A) =
            X(s.upsert(id,a))
        }
      }
    }
  }
}
