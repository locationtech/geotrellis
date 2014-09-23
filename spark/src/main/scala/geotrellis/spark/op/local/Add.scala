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
import geotrellis.raster.op.local.Add
import geotrellis.spark.rdd.TmsRasterRDD

trait AddOpMethods[+Repr <: TmsRasterRDD] { self: Repr =>
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) = 
    self.mapTiles { case TmsTile(t, r) => TmsTile(t, Add(r, i)) }
  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) = 
    self.mapTiles { case TmsTile(t, r) => TmsTile(t, Add(r, d)) }
  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)
  /** Add the values of each cell in each raster.  */
  def localAdd(rdd: TmsRasterRDD) =
    self.combineTiles(rdd) { case (TmsTile(t1, r1), TmsTile(t2, r2)) => TmsTile(t1, Add(r1, r2)) }
  /** Add the values of each cell in each raster. */
  def +(rdd: TmsRasterRDD) = localAdd(rdd)
}
