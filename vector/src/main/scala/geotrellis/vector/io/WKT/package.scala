package geotrellis.vector.io

import geotrellis.vector._

package object wkt {
  implicit class WktWrapper(val g: Geometry) {
    def toWKT(): String =
      WKT.write(g)
  }

  implicit class WktStringWrapper(val s: String) {
    def parseWKT[G <: Geometry](): G =
      WKT.read(s)
  }
}
