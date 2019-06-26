package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiPointMethods extends MethodExtensions[MultiPoint] {
  def points: Array[Point] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Point]
    }
  }.toArray

  def typedIntersection(p: Point): PointOrNoResult = self.intersection(p)
  def typedIntersection(mp: MultiPoint): MultiPointMultiPointIntersectionResult = self.intersection(mp)
  def typedIntersection[G <: Geometry : AtLeastOneDimension](g: G): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(g)
  def typedIntersection(ex: Extent): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): MultiPointGeometryDifferenceResult = self.difference(g)

  def |(p: Point): PointZeroDimensionsUnionResult = self.union(p)
  def |(mp: MultiPoint): MultiPointMultiPointUnionResult = self.union(mp)
  def |(l: LineString): ZeroDimensionsLineStringUnionResult = self.union(l)
  def |(ml: MultiLineString): MultiPointMultiLineStringUnionResult= self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): MultiPointMultiPolygonUnionResult = self.union(mp)

  def normalized(): MultiPoint = {
    val res = self.copy.asInstanceOf[MultiPoint]
    res.normalize
    res
  }
}
