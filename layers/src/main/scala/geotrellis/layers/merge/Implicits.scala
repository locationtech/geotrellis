package geotrellis.layers.merge

import geotrellis.util.MethodExtensions


object Implicits extends Implicits

trait Implicits {
  implicit class withMergableMethods[T: Mergable](val self: T) extends MethodExtensions[T] {
    def merge(other: T): T =
      implicitly[Mergable[T]].merge(self, other)
  }
}
