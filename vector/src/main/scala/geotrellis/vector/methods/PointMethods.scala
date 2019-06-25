package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraPointMethods extends MethodExtensions[Point] {
  def x: Double = self.getCoordinate.getX
  def y: Double = self.getCoordinate.getY

  def typedIntersection(g: Geometry): PointOrNoResult = self.intersection(g)
  def typedIntersection(ex: Extent): PointOrNoResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): PointGeometryDifferenceResult = self.difference(g)
}
