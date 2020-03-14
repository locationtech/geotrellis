/*
 * Copyright 2020 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
