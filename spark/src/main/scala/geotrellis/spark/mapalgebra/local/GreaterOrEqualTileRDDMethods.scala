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
import geotrellis.raster.mapalgebra.local.GreaterOrEqual
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait GreaterOrEqualTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqual(i: Int) =
    self.mapValues { r => GreaterOrEqual(r, i) }

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def localGreaterOrEqualRightAssociative(i: Int) =
    self.mapValues { r => GreaterOrEqual(i, r) }

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=(i: Int) = localGreaterOrEqual(i)

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input integer, else 0.
    */
  def >=:(i: Int) = localGreaterOrEqualRightAssociative(i)

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqual(d: Double) =
    self.mapValues { r => GreaterOrEqual(r, d) }

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or equal
    * to the input double, else 0.
    */
  def localGreaterOrEqualRightAssociative(d: Double) =
    self.mapValues { r => GreaterOrEqual(d, r) }

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=(d: Double) = localGreaterOrEqual(d)

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than or
    * equal to the input double, else 0.
    */
  def >=:(d: Double) = localGreaterOrEqualRightAssociative(d)

  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def localGreaterOrEqual(other: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(GreaterOrEqual.apply)
  
  /**
    * Returns a TileLayerRDD with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than or equal
    * to the next raster, else 0.
    */
  def >=(other: RDD[(K, Tile)]) = localGreaterOrEqual(other)
}
