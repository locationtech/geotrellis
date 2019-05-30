package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraMultiPolygonMethods extends MethodExtensions[MultiPolygon] {
  def polygons: Array[Polygon] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Polygon]
    }
  }.toArray

  def typedIntersection(p: Point): PointOrNoResult = self.intersection(p)
  def typedIntersection[G <: Geometry : TwoDimensional](g: G): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(g)
  def typedIntersection[G <: Geometry : OneDimensional](g: G): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(g)
}
