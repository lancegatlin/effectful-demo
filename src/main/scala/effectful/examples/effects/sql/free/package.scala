package effectful.examples.effects.sql

import effectful.Free

package object free {
  type FreeSqlDriverCmd[A] = Free[SqlDriverCmd,A]
}
