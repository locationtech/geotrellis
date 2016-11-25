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
import geotrellis.raster.mapalgebra.local.Less
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait LessTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLess(i: Int) =
    self.mapValues { r => Less(r, i) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def <(i: Int) = localLess(i)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    */
  def localLessRightAssociative(i: Int) =
    self.mapValues { r => Less(i, r) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * integer, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(i: Int) = localLessRightAssociative(i)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLess(d: Double) =
    self.mapValues { r => Less(r, d) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def localLessRightAssociative(d: Double) =
    self.mapValues { r => Less(d, r) }
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    */
  def <(d: Double) = localLess(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is less than the input
    * double, else 0.
    *
    * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
    */
  def <<:(d: Double) = localLessRightAssociative(d)
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def localLess(other: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Less.apply)

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are less than the next
    * raster, else 0.
    */
  def <(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localLess(other)
}
