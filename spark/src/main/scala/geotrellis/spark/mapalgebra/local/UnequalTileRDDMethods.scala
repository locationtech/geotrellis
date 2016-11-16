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
import geotrellis.raster.mapalgebra.local.Unequal
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait UnequalTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int) =
    self.mapValues { r => Unequal(r, i) }

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==:(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double) =
    self.mapValues { r => Unequal(r, d) }

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(other: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Unequal.apply)

  /**
   * Returns a Tile with data of BitCellType, where cell values equal 1 if
   * the corresponding cell valued of the raster are not equal, else 0.
   */
  def !==(other: RDD[(K, Tile)]) = localUnequal(other)
}
