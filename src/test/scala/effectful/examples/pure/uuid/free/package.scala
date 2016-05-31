package effectful.examples.pure.uuid

import effectful.free.Free

package object free {
  type FreeUUIDCmd[A] = Free[UUIDCmd,A]
}
