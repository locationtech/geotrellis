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

import geotrellis.raster._
import geotrellis.engine._

/** Computes the median value of a neighborhood for a given raster 
 *
 * @param    r      Tile on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.
 *
 * @note    Median does not currently support Double raster data.
 *          If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *          the data values will be rounded to integers.
 */
case class Median(r: Op[Tile], n: Op[Neighborhood], tns: Op[TileNeighbors] = TileNeighbors.NONE)
    extends FocalOperation0[Tile](r, n, tns)
{
  override def getCalculation(r: Tile, n: Neighborhood) = MedianCalculation(r, n)
}