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
import geotrellis.raster._
import geotrellis.raster.op.local.Multiply

trait MultiplyRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int): RasterRDD[K] =
    rasterRDD.mapTiles { case (t, r) => (t, Multiply(r, i)) }
  /** Multiply a constant value from each cell.*/
  def *(i: Int): RasterRDD[K] = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i: Int): RasterRDD[K] = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double): RasterRDD[K] =
    rasterRDD.mapTiles { case (t, r) => (t, Multiply(r, d)) }
  /** Multiply a double constant value from each cell.*/
  def *(d: Double): RasterRDD[K] = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d: Double): RasterRDD[K] = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(other: RasterRDD[K]): RasterRDD[K] =
    rasterRDD.combineTiles(other) {
      case ((t1, r1), (t2, r2)) => (t1, Multiply(r1, r2))
    }
  /** Multiply the values of each cell in each raster. */
  def *(other: RasterRDD[K]): RasterRDD[K] = localMultiply(other)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(others: Seq[RasterRDD[K]]): RasterRDD[K] =
    rasterRDD.combineTiles(others) {
      case tiles =>
        (tiles.head.id, Multiply(tiles.map(_.tile)))
    }
  /** Multiply the values of each cell in each raster. */
  def *(others: Seq[RasterRDD[K]]): RasterRDD[K] = localMultiply(others)
}
