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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.engine._

import Angles._

/** Creates [[Slope]] instances. */
object Slope {
  /**
   * Creates a slope operation with a default zFactor of 1.0.
   *
   * @param   raster     Tile for which to compute the aspect.
   * @param      cellSize  cellSize of the raster
   */
  def apply(r: Op[Tile], cs: Op[CellSize]): Slope =
    new Slope(r, TileNeighbors.NONE, cs, 1.0)

  /**
   * Creates a slope operation with a default zFactor of 1.0.
   *
   * @param   raster     Tile for which to compute the aspect.
   * @param      tns     TileNeighbors that describe the neighboring tiles.
   * @param      cellSize  cellSize of the raster
   */
  def apply(r: Op[Tile], tns: Op[TileNeighbors], cs: Op[CellSize]): Slope =
    new Slope(r, tns, cs, 1.0)

  /**
   * Creates a slope operation.
   *
   * @param   raster     Tile for which to compute the aspect.
   * @param   cellSize  cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   *                     The z factor is the multiplicative factor to convert elevation units
   */
  def apply(r: Op[Tile], cs: Op[CellSize], zFactor: Op[Double])(implicit di: DummyImplicit): Slope =
    new Slope(r, TileNeighbors.NONE, cs, zFactor)

  /**
   * Creates a slope operation.
   *
   * @param   raster     Tile for which to compute the aspect.
   * @param   tns        TileNeighbors that describe the neighboring tiles.
   * @param   cellSize   cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   *                     The z factor is the multiplicative factor to convert elevation units
   */
  def apply(r: Op[Tile], tns: Op[TileNeighbors], cs: Op[CellSize], zFactor: Op[Double]): Slope =
    new Slope(r, tns, cs, zFactor)
}

/** Calculates the slope of each cell in a raster.
  *
  * Slope is the magnitude portion of the gradient vector. It is the maximum
  * change of elevation from a raster cell to any immediate neighbor. It uses Horn's method
  * for computing slope.
  * 
  * As with aspect, slope is calculated from estimates of the partial derivatives dz / dx and dz / dy.
  *
  * Slope is computed in degrees from horizontal.
  * 
  * The expression for slope is:
  * {{{
  * val slope = atan(sqrt(pow(`dz / dy`, 2) * pow(`dz / dx`, 2)))
  * }}}
  *
  * @param   raster     Tile for which to compute the aspect.
  * @param   cellSize   cellSize of the raster
  * @param   zFactor    Number of map units to one elevation unit.
  *                     The z factor is the multiplicative factor to convert elevation units
  * 
  * @see [[SurfacePoint]] for slope calculation logic.
  * @see [[http://goo.gl/JCnNP Geospatial Analysis - A comprehensive guide]]
  * (Smit, Longley, and Goodchild)
  */
class Slope(r: Op[Tile], ns: Op[TileNeighbors], cellSize: Op[CellSize], zFactor: Op[Double]) 
    extends FocalOp2[CellSize, Double, Tile](r, Square(1), ns, cellSize, zFactor)({
  (r, n) => new SurfacePointCalculation[Tile] with DoubleArrayTileResult 
                                              with Initialization2[CellSize, Double] {
    var zFactor = 0.0

    override def init(r: Tile, cs: CellSize, z: Double) = {
      super.init(r)
      cellSize = cs
      zFactor = z
    }

    def setValue(x: Int, y: Int, s: SurfacePoint) {
      tile.setDouble(x, y, degrees(s.slope(zFactor)))
    }
  }
})
