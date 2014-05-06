package geotrellis.slick

import geotrellis.proj4._
import geotrellis.feature.Geometry

case class ProjectedGeometry[G <: Geometry](srid: Int, geom: G)