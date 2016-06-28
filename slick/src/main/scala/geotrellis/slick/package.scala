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

package geotrellis

import geotrellis.vector._

/**
  * Implicit conversion for geotrellis.vector.Geometry instances.
  *
  * @example {{{
  * import geotrellis.vector._
  * import geotrellis.slick._
  *
  * // create a web mercator projected point with the ExtendGeometry implicit class
  * val projectedPoint = Point(1.0, 2.0).withSRID(3274)
  * }}}
  */
package object slick {
  implicit class ExtendGeometry[G <: Geometry](g: G) {
    /** Upgrade Geometry to Projected[Geometry] */
    def withSRID(srid: Int) = Projected(g, srid)
  }
}
