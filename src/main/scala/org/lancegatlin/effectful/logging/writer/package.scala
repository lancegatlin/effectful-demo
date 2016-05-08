package org.lancegatlin.effectful.logging

package object writer {
  type LogWriter[A] = scalaz.Writer[List[LogEntry],A]
}
