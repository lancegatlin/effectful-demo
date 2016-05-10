package effectful.examples.effects.sql

import effectful.Free

package object free {
  type FreeSqlCmd[A] = Free[SqlCmd,A]
}
