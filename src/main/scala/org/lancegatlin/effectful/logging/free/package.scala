package org.lancegatlin.effectful.logging

import org.lancegatlin.effectful.Free

package object free {
  type FreeLogging[A] = Free[LoggingCmd,A]
}
