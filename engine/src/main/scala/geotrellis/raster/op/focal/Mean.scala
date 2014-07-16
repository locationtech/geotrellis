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

/** Computes the mean value of a neighborhood for a given raster. Returns a raster of TypeDouble
 *
 * @param    r      Tile on which to run the focal operation.
 * @param    n      Neighborhood to use for this operation (e.g., [[Square]](1))
 * @param    tns    TileNeighbors that describe the neighboring tiles.

 * @return          Returns a double value raster that is the computed mean for each neighborhood.
 *
 * @note            If the neighborhood is a [[Square]] neighborhood, the mean calucation will use
 *                  the [[CellwiseMeanCalc]] to perform the calculation, because it is faster.
 *                  If the neighborhood is of any other type, then [[CursorMeanCalc]] is used.
 */
case class Mean(r: Op[Tile], n: Op[Neighborhood], tns: Op[TileNeighbors] = TileNeighbors.NONE)
    extends FocalOp[Tile](r, n, tns)(MeanCalculation.apply)
