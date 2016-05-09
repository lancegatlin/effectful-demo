package effectful.examples.effects.db.inmemory

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import effectful._
import effectful.examples.effects.db.Db
import effectful.examples.effects.db.Db.RecordMetadata
import effectful.examples.effects.db.query.Query

object InMemoryDbImpl {
  class DuplicateKeyException(message: String) extends Exception(message)
}

class InMemoryDbImpl[ID,A](
  initial: Map[ID,A]
) extends Db[ID,A,Id] {
  import InMemoryDbImpl._

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
            data + (id ->(id, value, metadata))
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
  override def update(records: TraversableOnce[(ID, A)]): Id[Int] =
    records.foldLeft(0) { case (acc,(id,record)) => if(update(id,record)) 1 else 0 }

  override def insert(id: ID, value: A): Id[Boolean] = {
    try {
      data = { data =>
        data.get(id) match {
          case Some(_) =>
            throw new DuplicateKeyException(s"Key $id already exists")
          case None =>
            data + (id ->(id, value, mkMetadata()))
        }
      }
      true
    } catch {
      case _:DuplicateKeyException =>
        false
    }
  }

  override def insert(records: Seq[(ID, A)]): Id[Int] =
    records.foldLeft(0) { case (acc,(id,record)) => if(insert(id,record)) 1 else 0 }

  override def remove(id: ID): Id[Boolean] =
    if(data.contains(id)) {
      data = _ - id
      true
    } else {
      false
    }

  override def remove(ids: TraversableOnce[ID]): Id[Int] =
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
      data + (id ->(id, value, mkMetadata()))
    }
    (inserted,updated)
  }

  override def upsert(records: TraversableOnce[(ID, A)]): (Int, Int) =
    records.foldLeft((0,0)) { case ((inserted,updated),(id,record)) =>
        val (i,u) = upsert(id,record)
      (if(i) inserted + 1 else inserted, if(u) updated + 1 else updated)
    }


  override def find(query: Query[A]): Id[Seq[(ID, A, RecordMetadata)]] = {
    val q = Query.mkInMemoryQuery(query)
    data.valuesIterator.filter { case (_,record,_) => q(record) }.toSeq
  }
}
