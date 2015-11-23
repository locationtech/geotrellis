package geotrellis.vector.io

import geotrellis.vector._

package object wkb {
  implicit class WKBWrapper(val g: Geometry) {
    def toWKB(srid: Int = 0): Array[Byte] =
      WKB.write(g, srid)
  }
}
