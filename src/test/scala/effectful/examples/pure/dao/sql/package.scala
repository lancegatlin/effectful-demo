package effectful.examples.pure.dao

import effectful.examples.effects.sql._

package object sql {
  implicit class CharDataPML(val self: CharData) extends AnyVal {
    def to[A](implicit fmt: CharDataFormat[A]) : A =
      fmt.fromCharData(self)
  }

  implicit class BinDataPML(val self: BinData) extends AnyVal {
    def to[A](implicit fmt: BinDataFormat[A]) : A =
      fmt.fromBinData(self)
  }

  implicit class EverythingPML[A](val self: A) extends AnyVal {
    def toCharData(implicit fmt: CharDataFormat[A]) : CharData =
      fmt.toCharData(self)
    def toBinData(implicit fmt: BinDataFormat[A]) : BinData =
      fmt.toBinData(self)
  }

}
