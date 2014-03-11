/***
 * Copyright (c) 2014 Digital Globe.
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
 ***/

package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster.op.local.Multiply
import geotrellis.spark.rdd.RasterRDD

trait MultiplyOpMethods[+Repr <: RasterRDD] { self: Repr =>
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Multiply(r, i)) }
  /** Multiply a constant value from each cell.*/
  def *(i:Int) = localMultiply(i)
  /** Multiply a constant value from each cell.*/
  def *:(i:Int) = localMultiply(i)
  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) = 
    self.mapTiles { case Tile(t, r) => Tile(t, Multiply(r, d)) }
  /** Multiply a double constant value from each cell.*/
  def *(d:Double) = localMultiply(d)
  /** Multiply a double constant value from each cell.*/
  def *:(d:Double) = localMultiply(d)
  /** Multiply the values of each cell in each raster. */
  def localMultiply(rdd: RasterRDD) = 
    self.combineTiles(rdd) { case (Tile(t1, r1), Tile(t2, r2)) => Tile(t1, Multiply(r1, r2)) }
  /** Multiply the values of each cell in each raster. */
  def *(rdd: RasterRDD) = localMultiply(rdd)
}
