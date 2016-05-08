package org.lancegatlin.example

import java.time.Instant

case class RecordMetadata(
  created: Instant,
  lastUpdated: Instant,
  deleted: Option[Instant]
)
