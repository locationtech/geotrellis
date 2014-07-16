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

/** Calculates spatial autocorrelation of cells based on the similarity to
 * neighboring values.
 *
 * The statistic for each focus in the resulting raster is such that the more
 * positive the number, the greater the similarity between the focus value and
 * it's neighboring values, and the more negative the number, the more dissimilar
 * the focus value is with it's neighboring values.
 *
 * @param       r         Tile to perform the operation on.
 * @param       n         Neighborhood to use in this focal operation.
 * @param       tns       TileNeighbors that describe the neighboring tiles.
 *
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 * 
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (TypeFloat, TypeDouble).
 */
case class TileMoransI(r: Op[Tile], n: Op[Neighborhood], tns: Op[TileNeighbors] = TileNeighbors.NONE)
    extends FocalOp[Tile](r, n, tns)(TileMoransICalculation.apply)

/** Calculates global spatial autocorrelation of a raster based on the similarity to
 * neighboring values.
 *
 * The resulting statistic is such that the more positive the number, the greater
 * the similarity of values in the raster, and the more negative the number,
 * the more dissimilar the raster values are.
 *
 * @param       r         Tile to perform the operation on.
 * @param       n         Neighborhood to use in this focal operation.
 * @param       tns       TileNeighbors that describe the neighboring tiles.
 * 
 * @note                  This operation requires that the whole raster be passed in;
 *                        it does not work over tiles.
 *
 * @note                  Since mean and standard deviation are based off of an
 *                        Int based Histogram, those values will come from rounded values
 *                        of a double typed Tile (TypeFloat, TypeDouble).
 */
case class ScalarMoransI(r: Op[Tile], n: Op[Neighborhood], tns: Op[TileNeighbors] = TileNeighbors.NONE)
  extends FocalOp(r, n, tns)(ScalarMoransICalculation.apply)
