package effectful.examples.effects.sql

import effectful.free.Free

package object free {
  type FreeSqlDriverCmd[A] = Free[SqlDriverCmd,A]
}
