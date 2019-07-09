package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions
import org.locationtech.jts.geom.TopologyException

trait ExtraGeometryMethods extends MethodExtensions[Geometry] {
  def extent: Extent =
    if(self.isEmpty) Extent(0.0, 0.0, 0.0, 0.0)
    else self.getEnvelopeInternal

  def &(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(g)

  def intersectionSafe(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    try self.intersection(g)
    catch {
      case _: TopologyException => GeomFactory.simplifier.reduce(self).intersection(GeomFactory.simplifier.reduce(g))
    }
}
