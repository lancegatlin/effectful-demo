package effectful.impl

import effectful.Id
import effectful.cats.Capture

trait IdCapture extends Capture[Id] {
  def capture[A](a: => A) = a
}
