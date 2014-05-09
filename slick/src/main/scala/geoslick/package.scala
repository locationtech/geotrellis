package geotrellis

import geotrellis.feature._

package object slick {
  implicit class ExtendGeometry[G <: Geometry](g: G) {
    /** Upgrade Geometry to Projected[Geometry] */
    def withSRID(srid: Int) = Projected(g, srid)
  }
}