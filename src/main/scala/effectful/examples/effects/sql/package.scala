package effectful.examples.effects

package object sql {
  type Row = Int => SqlVal
}
