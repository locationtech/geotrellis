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

package geotrellis.vector

import GeomFactory._
import geotrellis.proj4.{CRS, Transform}

/** An extent and its corresponding CRS
  *
  * @param extent The Extent which is projected
  * @param crs    The CRS projection of this extent
  */
case class ProjectedExtent(extent: Extent, crs: CRS) {
  def reproject(dest: CRS): Extent =
    extent.reproject(crs, dest)

  def reprojectAsPolygon(dest: CRS, relError: Double = 0.01): Polygon =
    extent.reprojectAsPolygon(Transform(crs, dest), relError)
}

object ProjectedExtent {
  implicit def fromTupleA(tup: (Extent, CRS)):ProjectedExtent = ProjectedExtent(tup._1, tup._2)
  implicit def fromTupleB(tup: (CRS, Extent)):ProjectedExtent = ProjectedExtent(tup._2, tup._1)
}
