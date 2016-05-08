package org.lancegatlin.example

import java.time.Instant

import scala.language.higherKinds

trait EntityService[ID,A,E[_]] {
  import EntityService._

  /** @return if id exists, some value otherwise none */
  def findById(id: ID) : E[Option[(A,RecordMetadata)]]
  /** @return a batch of all records starting with specified index and batch size (records are sorted by creation time) */
  def findAll(start: Int, batchSize: Int) : E[Seq[(A,RecordMetadata)]]
  /** @return TRUE if document was inserted FALSE if id already exists */
  def insert(id: ID, a : A) : E[Boolean]
  /** @return TRUE if document was updated FALSE if id doesn't exist */
  def update(id: ID, value: A) : E[Boolean]

  def upsert(id: ID, a : A) : E[Unit]
  /** @return TRUE if document was marked as removed FALSE if id doesn't exist */
  def remove(id: ID) : E[Boolean]
}

object EntityService {
  case class RecordMetadata(
    created: Instant,
    lastUpdated: Instant,
    removed: Option[Instant]
  )
}