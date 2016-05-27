package effectful.examples.effects.logging

import effectful.free.Free

package object free {
  type FreeLoggingCmd[A] = Free[LoggingCmd,A]
}
