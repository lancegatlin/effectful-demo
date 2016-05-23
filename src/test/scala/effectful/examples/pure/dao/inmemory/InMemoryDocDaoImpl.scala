package effectful.examples.pure.dao.inmemory

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import effectful._
import effectful.examples.pure.dao.DocDao
import effectful.examples.pure.dao.DocDao.RecordMetadata
import effectful.examples.pure.dao.query.Query

object InMemoryDocDaoImpl {
  class DuplicateKeyException(message: String) extends Exception(message)
}

class InMemoryDocDaoImpl[ID,A](
  initial: Map[ID,A]
) extends DocDao[ID,A,Id] {
  import InMemoryDocDaoImpl._

  def mkMetadata() = RecordMetadata(
    created = Instant.now,
    lastUpdated = Instant.now,
    removed = None
  )

  type Data = Map[ID,(ID,A,RecordMetadata)]
  val _data = new AtomicReference[Data](
    initial.map { case (id,record) =>
      (id,(id,record,mkMetadata()))
    }
  )
  def data = _data.get
  def data_=(f: Data => Data) : Unit = {
    var result = false
    do {
      val current = _data.get
      val next = f(current)
      result = _data.compareAndSet(current,next)
    } while(result == false)
  }


  override def exists(id: ID): Id[Boolean] =
    data.contains(id)

  override def batchExists(ids: Traversable[ID]): Id[Set[ID]] =
    ids.toSet intersect data.keySet

  override def findById(id: ID): Id[Option[(ID, A, RecordMetadata)]] =
    data.get(id)

  override def findAll(start: Int, batchSize: Int): Id[Seq[(ID, A, RecordMetadata)]] =
    // todo: cache this
    data.toSeq.map(_._2).sortBy(-_._3.created.toEpochMilli).slice(start,start + batchSize)

  override def update(id: ID, value: A): Id[Boolean] = {
    try {
      data = { data =>
        data.get(id) match {
          case Some((id, record, metadata)) =>
            val tuple = (id, value, metadata)
            data + (id -> tuple)
          case None =>
            throw new NoSuchElementException(s"Missing key $id")
        }
      }
      true
    } catch {
      case _:NoSuchElementException =>
        false
    }
  }
  override def batchUpdate(records: Traversable[(ID, A)]): Id[Int] =
    records.foldLeft(0) { case (acc,(id,record)) => if(update(id,record)) 1 else 0 }

  override def insert(id: ID, value: A): Id[Boolean] = {
    try {
      data = { data =>
        data.get(id) match {
          case Some(_) =>
            throw new DuplicateKeyException(s"Key $id already exists")
          case None =>
            val tuple = (id, value, mkMetadata())
            data + (id -> tuple)
        }
      }
      true
    } catch {
      case _:DuplicateKeyException =>
        false
    }
  }

  override def batchInsert(records: Traversable[(ID, A)]): Id[Int] =
    records.foldLeft(0) { case (acc,(id,record)) => if(insert(id,record)) 1 else 0 }

  override def remove(id: ID): Id[Boolean] =
    if(data.contains(id)) {
      data = _ - id
      true
    } else {
      false
    }

  override def batchRemove(ids: Traversable[ID]): Id[Int] =
    ids.foldLeft(0)((acc,id) => if(remove(id)) 1 else 0)

  override def upsert(id: ID, value: A): (Boolean, Boolean) = {
    var inserted = false
    var updated = false

    data = { data =>
      data.get(id) match {
        case Some(_) =>
          updated = true
        case None =>
          inserted = true
      }
      val tuple = (id, value, mkMetadata())
      data + (id -> tuple)
    }
    (inserted,updated)
  }

  override def batchUpsert(records: Traversable[(ID, A)]): (Int, Int) =
    records.foldLeft((0,0)) { case ((inserted,updated),(id,record)) =>
        val (i,u) = upsert(id,record)
      (if(i) inserted + 1 else inserted, if(u) updated + 1 else updated)
    }


  override def find(query: Query[A]): Id[Seq[(ID, A, RecordMetadata)]] = {
    val q = Query.mkInMemoryQuery(query)
    data.valuesIterator.filter { case (_,record,_) => q(record) }.toSeq
  }
}
