package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions

trait ExtraGeometryMethods extends MethodExtensions[Geometry] {
  def envelope: Extent =
    if(self.isEmpty) Extent(0.0, 0.0, 0.0, 0.0)
    else self.getEnvelopeInternal

  def typedIntersection(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(g)
}
