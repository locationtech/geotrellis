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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.hillshade.{SurfacePoint, SurfacePointCalculation}
import geotrellis.raster.mapalgebra.focal.Angles._

/**
  * Calculates the slope of each cell in a raster.
  *
  * Slope is the magnitude portion of the gradient vector. It is the maximum
  * change of elevation from a raster cell to any immediate neighbor. It uses Horn's method
  * for computing slope.
  *
  * As with aspect, slope is calculated from estimates of the partial derivatives dz / dx and dz / dy.
  *
  * Slope is computed in degrees from horizontal.
  *
  * The use of a z-factor is essential for correct slope calculations when the surface z units are expressed in units different from the ground x,y units.
  *
  * If Slope operations encounters NoData in its neighborhood, that neighborhood cell well be treated as having
  * the same elevation as the focal cell.
  *
  * The expression for slope is:
  * {{{
  * val slope = atan(sqrt(pow(`dz / dy`, 2) * pow(`dz / dx`, 2)))
  * }}}
  *
  */
object Slope {

  def apply(r: Tile, n: Neighborhood, bounds: Option[GridBounds], cs: CellSize, z: Double, target: TargetCell = TargetCell.All): Tile = {
    new SurfacePointCalculation[Tile](r, n, bounds, cs, target)
      with DoubleArrayTileResult
    {
      val zFactor = z

      def setValue(x: Int, y: Int, s: SurfacePoint) {
        resultTile.setDouble(x, y, degrees(s.slope(zFactor)))
      }
    }
  }.execute()
}
