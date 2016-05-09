package org.lancegatlin.example.logging

package object writer {
  type LogWriter[A] = scalaz.Writer[List[LogEntry],A]
}
