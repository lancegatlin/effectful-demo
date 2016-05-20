package effectful.examples.pure.dao

import java.time.Instant

import effectful._
import query.Query

import scala.language.higherKinds

/**
  * A simple document store
  *
  * @tparam ID type of the identifier
  * @tparam A type of the record
  * @tparam E effect system
  */
trait DocDao[ID,A,E[_]] {
  import DocDao._

  /** @return TRUE if id is in use FALSE otherwise */
  def exists(id: ID) : E[Boolean]
  /** @return empty sequence if all IDs exists otherwise the set of IDs that exist */
  def batchExists(ids: Traversable[ID]) : E[Set[ID]]

  /** @return if id exists, some value otherwise none */
  def findById(id: ID) : E[Option[(ID,A,RecordMetadata)]]

  def find(query: Query[A]) : E[Seq[(ID,A,RecordMetadata)]]

  /** @return a batch of all records starting with specified index and batch size (records are sorted by creation time) */
  def findAll(start: Int, batchSize: Int) : E[Seq[(ID,A,RecordMetadata)]]

  /** @return TRUE if document was inserted FALSE if id already exists */
  def insert(id: ID, a : A) : E[Boolean]
  /** @return count of documents successfully inserted */
  def batchInsert(records: Traversable[(ID,A)]) : E[Int]
  /** @return TRUE if document was updated FALSE if id doesn't exist */
  def update(id: ID, value: A) : E[Boolean]

  def batchUpdate(records: Traversable[(ID,A)]) : E[Int]

  def upsert(id: ID, a : A) : E[(Boolean,Boolean)]
  def batchUpsert(records: Traversable[(ID,A)]) : E[(Int,Int)]

  /** @return TRUE if document was marked as removed FALSE if id doesn't exist */
  def remove(id: ID) : E[Boolean]
  def batchRemove(ids: Traversable[ID]) : E[Int]
}

object DocDao {
  case class RecordMetadata(
    created: Instant,
    lastUpdated: Instant,
    removed: Option[Instant]
  )
  object RecordMetadata {
    def forNewRecord = RecordMetadata(
      created = Instant.now(),
      lastUpdated = Instant.now(),
      removed = None
    )
  }
  
  implicit def liftS_DocDao[ID,A] = {
    type S[E[_]] = DocDao[ID,A,E]

    new LiftS[S] {
    override def apply[E[_], F[_]](
      s: DocDao[ID,A,E]
    )(implicit
      E: EffectSystem[E],
      F: EffectSystem[F],
      liftE: LiftE[E, F]
    ): DocDao[ID,A,F] =
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