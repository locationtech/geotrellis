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
import geotrellis.raster.op.local.Multiply
import geotrellis.raster.Tile

trait MultiplyRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Multiply(r, i)) }
  /** Multiply a constant value from each cell.*/
  def *(i: Int): RasterRDD[K, Tile] = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int): RasterRDD[K, Tile] = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double): RasterRDD[K, Tile] =
    rasterRDD.mapPairs { case (t, r) => (t, Multiply(r, d)) }
  /** Multiply a double constant value from each cell.*/
  def *(d: Double): RasterRDD[K, Tile] = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double): RasterRDD[K, Tile] = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] =
    rasterRDD.combineTiles(other) { case (t1, t2) => Multiply(t1, t2) }
  /** Multiply the values of each cell in each raster. */
  def *(other: RasterRDD[K, Tile]): RasterRDD[K, Tile] = localMultiply(other)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others) {
      case tiles =>
        (tiles.head.id, Multiply(tiles.map(_.tile)))
    }
  /** Multiply the values of each cell in each raster. */
  def *(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] = localMultiply(others)
}
