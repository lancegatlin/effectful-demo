package effectful.examples.effects.logging

import effectful.free.Free

package object free {
  type FreeLoggerCmd[A] = Free[LoggerCmd,A]
}
