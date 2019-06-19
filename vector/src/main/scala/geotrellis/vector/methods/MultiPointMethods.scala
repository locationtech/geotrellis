package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiPointMethods extends MethodExtensions[MultiPoint] {
  def points: Array[Point] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Point]
    }
  }.toArray
}
