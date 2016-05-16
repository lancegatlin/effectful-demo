package effectful.examples.effects.logging

import effectful.Free

package object free {
  type FreeLoggingCmd[A] = Free[LoggingCmd,A]
}
