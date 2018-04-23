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

package geotrellis.vector.simplify

import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.locationtech.jts.{geom => jts}

trait SimplifyMethods[G <: Geometry] extends MethodExtensions[G] {
  /** Simplify the given geometry
    *
    * @param tolerance The tolerance, in distance, for ignoring small variations. More tolerance
    *                  means a simpler polygon and more edges removed
    * @note Does not preserve topology: polygons may be split and holes may be created
    */
  def simplify(tolerance: Double): G =
    org.locationtech.jts.simplify.VWSimplifier.simplify(self.jtsGeom, tolerance) match {
      case g: jts.Geometry if g.isEmpty => self
      case g => Geometry(g).asInstanceOf[G]
    }
}
