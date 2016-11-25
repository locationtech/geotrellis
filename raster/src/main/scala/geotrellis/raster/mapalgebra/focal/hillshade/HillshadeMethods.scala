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

package geotrellis.raster.mapalgebra.focal.hillshade

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.util.MethodExtensions


trait HillshadeMethods extends MethodExtensions[Tile] {
  /**
    * Computes Hillshade (shaded relief) from a raster.
    */
  def hillshade(cs: CellSize, azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1.0, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    Hillshade(self, Square(1), bounds, cs, azimuth, altitude, zFactor, target)
}
