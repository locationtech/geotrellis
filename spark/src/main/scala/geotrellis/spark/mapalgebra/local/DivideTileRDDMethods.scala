/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Divide
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait DivideTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int) =
    self.mapValues { r => Divide(r, i) }

  /** Divide each value of the raster by a constant value.*/
  def /(i: Int) = localDivide(i)

  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int) =
    self.mapValues { r => Divide(i, r) }

  /** Divide a constant value by each cell value.*/
  def /:(i: Int) = localDivideValue(i)

  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double) =
    self.mapValues { r => Divide(r, d) }

  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double) = localDivide(d)

  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double) =
    self.mapValues { r => Divide(d, r) }

  /** Divide a double constant value by each cell value.*/
  def /:(d: Double) = localDivideValue(d)

  /** Divide the values of each cell in each raster. */
  def localDivide(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localDivide(other, None)
  def localDivide(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Divide.apply)

  /** Divide the values of each cell in each raster. */
  def /(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localDivide(other, None)

  def localDivide(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localDivide(others, None)
  def localDivide(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Divide.apply)

  def /(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localDivide(others, None)
}
