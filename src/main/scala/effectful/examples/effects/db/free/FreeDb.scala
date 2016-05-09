package effectful.examples.effects.db.free

import effectful.Free
import effectful.examples.effects.db.Db
import effectful.examples.effects.db.Db.RecordMetadata
import effectful.examples.effects.db.query.Query

trait Context[ID,A] {
sealed trait DbCmd[R]

  case class FindById(id: ID) extends DbCmd[Option[(RecordMetadata)]]
  case class Find(query: Query[A]) extends DbCmd[Seq[(RecordMetadata)]]
  case class FindAll(start: Int, batchSize: Int) extends DbCmd[Seq[(RecordMetadata)]]
  case class Insert(id: ID, a : A) extends DbCmd[Boolean]
  case class BatchInsert(records: Seq[(ID,A)]) extends DbCmd[Int]
  case class Update(id: ID, value: A) extends DbCmd[Boolean]
  case class BatchUpdate(records: TraversableOnce[(ID,A)]) extends DbCmd[Int]
  case class Upsert(id: ID, a : A) extends DbCmd[(Boolean,Boolean)]
  case class BatchUpsert(records: TraversableOnce[(ID,A)]) extends DbCmd[(Int,Int)]
  case class Remove(id: ID) extends DbCmd[Boolean]
  case class BatchRemove(ids: TraversableOnce[ID]) extends DbCmd[Int]

  type FreeDbCmd[AA] = Free[DbCmd,AA]
}

class FreeDb[ID,A] extends Db[ID,A,Context[ID,A]#FreeDbCmd] with Context[ID,A] {

  override def findById(id: ID): FreeDbCmd[Option[(ID, A, RecordMetadata)]] =
    Free.Cmd(FindById(id))

  override def findAll(start: Int, batchSize: Int): FreeDbCmd[Seq[(ID, A, RecordMetadata)]] =
    Free.Cmd(FindAll(start,batchSize))

  override def batchRemove(ids: TraversableOnce[ID]): FreeDbCmd[Int] =
    Free.Cmd(BatchRemove(ids))

  override def update(id: ID, value: A): FreeDbCmd[Boolean] =
    Free.Cmd(Update(id,value))

  override def insert(id: ID, a: A): FreeDbCmd[Boolean] =
    Free.Cmd(Insert(id,a))

  override def remove(id: ID): FreeDbCmd[Boolean] =
    Free.Cmd(Remove(id))

  override def batchUpdate(records: TraversableOnce[(ID, A)]): FreeDbCmd[Int] =
    Free.Cmd(BatchUpdate(records))

  override def batchInsert(records: Seq[(ID, A)]): FreeDbCmd[Int] =
    Free.Cmd(BatchInsert(records))

  override def find(query: Query[A]): FreeDbCmd[Seq[(ID, A, RecordMetadata)]] =
    Free.Cmd(Find(query))

  override def upsert(id: ID, a: A): FreeDbCmd[(Boolean, Boolean)] =
    Free.Cmd(Upsert(id,a))

  override def batchUpsert(records: TraversableOnce[(ID, A)]): FreeDbCmd[(Int, Int)] =
    Free.Cmd(BatchInsert(records))

}
