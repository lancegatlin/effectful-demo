package effectful.examples.pure

import effectful._
import effectful.examples.pure.dao.query.Query

package object dao {
  implicit def liftS_DocDao[ID,A] = {
    type S[E[_]] = DocDao[ID,A,E]

    new LiftS[S] {
      override def apply[E[_], F[_]](
        s: DocDao[ID,A,E]
      )(implicit
        E: EffectSystem[E],
        F: EffectSystem[F],
        liftE: LiftE[E, F]
      ): DocDao[ID,A,F] = {
        import DocDao._
        new DocDao[ID,A,F] {
          override def exists(id: ID): F[Boolean] =
            liftE(s.exists(id))
          override def update(id: ID, value: A): F[Boolean] =
            liftE(s.update(id,value))
          override def batchRemove(ids: Traversable[ID]): F[Int] =
            liftE(s.batchRemove(ids))
          override def insert(id: ID, a: A): F[Boolean] =
            liftE(s.insert(id,a))
          override def findById(id: ID): F[Option[(ID, A, RecordMetadata)]] =
            liftE(s.findById(id))
          override def batchUpsert(records: Traversable[(ID, A)]): F[(Int, Int)] =
            liftE(s.batchUpsert(records))
          override def findAll(start: Int, batchSize: Int): F[Seq[(ID, A, RecordMetadata)]] =
            liftE(s.findAll(start,batchSize))
          override def batchExists(ids: Traversable[ID]): F[Set[ID]] =
            liftE(s.batchExists(ids))
          override def remove(id: ID): F[Boolean] =
            liftE(s.remove(id))
          override def batchUpdate(records: Traversable[(ID, A)]): F[Int] =
            liftE(s.batchUpdate(records))
          override def batchInsert(records: Traversable[(ID, A)]): F[Int] =
            liftE(s.batchInsert(records))
          override def find(query: Query[A]): F[Seq[(ID, A, RecordMetadata)]] =
            liftE(s.find(query))
          override def upsert(id: ID, a: A): F[(Boolean, Boolean)] =
            liftE(s.upsert(id,a))
        }
      }
    }
  }
}
