/*
 * Copyright 2016 Azavea
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

package geotrellis.vector.densify

import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.locationtech.jts.{geom => jts}

/** Trait used to implicitly extend [[Geometry]] instances with densifying methods */
trait DensifyMethods[G <: Geometry] extends MethodExtensions[G] {

  /** Add vertices along the line segments contained with a geometry
    *
    * @param tolerance  the upper bound on line segment lengths after densification
    */
  def densify(tolerance: Double): G =
    org.locationtech.jts.densify.Densifier.densify(self.jtsGeom, tolerance) match {
      case g: jts.Geometry if g.isEmpty => self
      case g => Geometry(g).asInstanceOf[G]
    }
}
