package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiLineStringMethods extends MethodExtensions[MultiLineString] {
  def lines: Array[LineString] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[LineString]
    }
  }.toArray

  def typedIntersection(p: Point): PointOrNoResult = self.intersection(p)
  def typedIntersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def typedIntersection[G <: Geometry : AtLeastOneDimension](g: G): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(g)
  def typedIntersection(ex: Extent): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): MultiLineStringGeometryDifferenceResult = self.difference(g)

  def normalized(): MultiLineString = {
    val res = self.copy.asInstanceOf[MultiLineString]
    res.normalize
    res
  }
}
