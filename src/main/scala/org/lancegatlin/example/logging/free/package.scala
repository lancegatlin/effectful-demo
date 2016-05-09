package org.lancegatlin.example.logging

import org.lancegatlin.effectful.Free

package object free {
  type FreeLogging[A] = Free[LoggingCmd,A]
}
