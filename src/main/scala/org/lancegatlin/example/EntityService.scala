package org.lancegatlin.example

import scala.language.higherKinds

trait EntityService[ID,A,E[_]] {
  /** @return if id exists, some value otherwise none */
  def findById(id: ID) : E[Option[(A,RecordMetadata)]]
  /** @return a batch of all records starting with specified index and batch size (records are sorted by creation time) */
  def findAll(start: Int, batchSize: Int) : E[Seq[(A,RecordMetadata)]]
  /** @return TRUE if document was inserted FALSE if id already exists */
  def insert(id: ID, a : A) : E[Boolean]
  /** @return TRUE if document was updated FALSE if id doesn't exist */
  def update(id: ID, value: A) : E[Boolean]
  /** @return TRUE if document was removed FALSE if id doesn't exist */
  def remove(id: ID) : E[Boolean]
}
