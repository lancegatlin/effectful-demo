package effectful.examples.effects.logging

import effectful.Free

package object free {
  type FreeLogging[A] = Free[LoggingCmd,A]
}
