package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraPointMethods extends MethodExtensions[Point] {
  def x: Double = self.getCoordinate.getX
  def y: Double = self.getCoordinate.getY

  def &(g: Geometry): PointOrNoResult = self.intersection(g)
  def &(ex: Extent): PointOrNoResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): PointGeometryDifferenceResult = self.difference(g)

  def |(p: Point): PointZeroDimensionsUnionResult = self.union(p)
  def |(mp: MultiPoint): PointZeroDimensionsUnionResult = self.union(mp)
  def |(l: LineString): ZeroDimensionsLineStringUnionResult = self.union(l)
  def |(ml: MultiLineString): PointMultiLineStringUnionResult = self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): PointMultiPolygonUnionResult = self.union(mp)
}
