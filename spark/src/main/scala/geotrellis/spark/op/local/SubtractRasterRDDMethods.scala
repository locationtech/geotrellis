/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Subtract
import geotrellis.raster.Tile

trait SubtractRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Subtract(r, i)) }
  /** Subtract a constant value from each cell.*/
  def -(i: Int): RasterRDD[K, Tile] = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Subtract(i, r)) }
  /** Subtract each value of a cell from a constant value. */
  def -:(i: Int): RasterRDD[K, Tile] = localSubtractFrom(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Subtract(r, d)) }
  /** Subtract a double constant value from each cell.*/
  def -(d: Double): RasterRDD[K, Tile] = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) =
    rasterRDD.mapPairs { case (t, r) => (t, Subtract(d, r)) }
  /** Subtract each value of a cell from a double constant value. */
  def -:(d: Double): RasterRDD[K, Tile] = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] =
    rasterRDD.combineTiles(other) { case (t1, t2) => Subtract(t1, t2) }
  /** Subtract the values of each cell in each raster. */
  def -(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localSubtract(other)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others) {
      case tiles =>
        (tiles.head.id, Subtract(tiles.map(_.tile)))
    }
  /** Subtract the values of each cell in each raster. */
  def -(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] = localSubtract(others)
}
