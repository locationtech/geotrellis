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
import geotrellis.spark.rdd.RasterRDD

trait SubtractRasterRDDMethods extends RasterRDDMethods {
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Subtract(r, i)) }
  /** Subtract a constant value from each cell.*/
  def -(i: Int): RasterRDD = localSubtract(i)
  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Subtract(i, r)) }
  /** Subtract each value of a cell from a constant value. */
  def -:(i: Int): RasterRDD = localSubtractFrom(i)
  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double): RasterRDD =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Subtract(r, d)) }
  /** Subtract a double constant value from each cell.*/
  def -(d: Double): RasterRDD = localSubtract(d)
  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) =
    rasterRDD.mapTiles { case TmsTile(t, r) => TmsTile(t, Subtract(d, r)) }
  /** Subtract each value of a cell from a double constant value. */
  def -:(d: Double): RasterRDD = localSubtractFrom(d)
  /** Subtract the values of each cell in each raster. */
  def localSubtract(other: RasterRDD): RasterRDD =
    rasterRDD.combineTiles(other) {
      case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Subtract(r1, r2))
    }
  /** Subtract the values of each cell in each raster. */
  def -(other: RasterRDD): RasterRDD = localSubtract(other)
  /** Subtract the values of each cell in each raster. */
}
