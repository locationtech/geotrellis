package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiLineStringMethods extends MethodExtensions[MultiLineString] {
  def lines: Array[LineString] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[LineString]
    }
  }.toArray
}
