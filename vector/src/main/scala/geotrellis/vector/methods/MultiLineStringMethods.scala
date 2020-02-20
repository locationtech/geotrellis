package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiLineStringMethods extends MethodExtensions[MultiLineString] {
  def lines: Array[LineString] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[LineString]
    }
  }.toArray

  def &(p: Point): PointOrNoResult = self.intersection(p)
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def &[G <: Geometry : AtLeastOneDimension](g: G): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(g)
  def &(ex: Extent): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): MultiLineStringGeometryDifferenceResult = self.difference(g)

  def |(p: Point): PointMultiLineStringUnionResult = self.union(p)
  def |(mp: MultiPoint): MultiPointMultiLineStringUnionResult = self.union(mp)
  def |(l: LineString): LineStringOneDimensionUnionResult = self.union(l)
  def |(ml: MultiLineString): MultiLineStringMultiLineStringUnionResult = self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): MultiLineStringMultiPolygonUnionResult = self.union(mp)

  def normalized(): MultiLineString = {
    val res = self.copy.asInstanceOf[MultiLineString]
    res.normalize
    res
  }
}
