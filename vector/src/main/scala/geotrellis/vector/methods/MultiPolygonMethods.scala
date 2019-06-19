package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiPolygonMethods extends MethodExtensions[MultiPolygon] {
  def polygons: Array[Polygon] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Polygon]
    }
  }.toArray
}
