package effectful.examples.adapter

import java.util.concurrent.ConcurrentHashMap

package object jdbc {
  implicit class ConcurrentHashMapPML[A,B](val self: ConcurrentHashMap[A,B]) extends AnyVal {
    def getOrDie(key: A) : B =
      Option(self.get(key)) match {
        case Some(v) => v
        case None => throw new NoSuchElementException(s"Missing key $key")
      }
    def apply(key: A) : B = getOrDie(key)
  }
}
