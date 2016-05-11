package geotrellis.vector.io.wkb

import geotrellis.util.MethodExtensions
import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class WKBWrapper(val self: Geometry) extends MethodExtensions[Geometry] {
    def toWKB(srid: Int = 0): Array[Byte] =
      WKB.write(self, srid)
  }

  implicit class WKBArrayWrapper(val self: Array[Byte]) extends MethodExtensions[Array[Byte]] {
    def readWKB(): Geometry =
      WKB.read(self)
  }

  implicit class WKHexStringWrapper(val self: String) extends MethodExtensions[String] {
    def readWKB(): Geometry =
      WKB.read(self)
  }
}
