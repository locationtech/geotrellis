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

package geotrellis.spark

import geotrellis.vector._
import geotrellis.proj4._

/**
  * This package is concerned with translation of coordinates or extents between
  * geographic extents and the grid space represented by SpatialKey(col, row) coordinates,
  * the layout that defines that grid space, as well as functionality for cutting tiles into
  * a uniform grid space.
  */
package object tiling {
  private final val WORLD_WSG84 = Extent(-180, -89.99999, 179.99999, 89.99999)

  implicit class CRSWorldExtent(crs: CRS) {
    def worldExtent: Extent =
      crs match {
        case LatLng =>
          WORLD_WSG84
        case WebMercator =>
          Extent(-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244)
        case Sinusoidal =>
          Extent(-2.0015109355797417E7, -1.0007554677898709E7, 2.0015109355797417E7, 1.0007554677898709E7)
        case _ =>
          WORLD_WSG84.reproject(LatLng, crs)
      }
  }
}
