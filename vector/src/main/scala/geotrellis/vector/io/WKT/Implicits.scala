package geotrellis.vector.io.wkt

import geotrellis.util.MethodExtensions
import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class WktWrapper(val self: Geometry) extends MethodExtensions[Geometry] {
    def toWKT(): String =
      WKT.write(self)
  }

  implicit class WktStringWrapper(val self: String) extends MethodExtensions[String] {
    def parseWKT(): Geometry =
      WKT.read(self)
  }
}
