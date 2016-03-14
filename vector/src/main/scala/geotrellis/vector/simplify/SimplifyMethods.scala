/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.vividsolutions.jts.{geom => jts}

trait SimplifyMethods[G <: Geometry] extends MethodExtensions[G] {
  def simplify(tolerance: Double): G =
    com.vividsolutions.jts.simplify.VWSimplifier.simplify(self.jtsGeom, tolerance) match {
      case g: jts.Geometry if g.isEmpty => self
      case g => Geometry(g).asInstanceOf[G]
    }
}
