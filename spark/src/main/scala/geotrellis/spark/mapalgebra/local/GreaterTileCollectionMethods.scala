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

import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.Greater
import geotrellis.util.MethodExtensions

trait GreaterTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreater(i: Int) =
    self.mapValues { r => Greater(r, i) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def localGreaterRightAssociative(i: Int) =
    self.mapValues { r => Greater(i, r) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    */
  def >(i: Int) = localGreater(i)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * integer, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(i: Int) = localGreaterRightAssociative(i)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreater(d: Double) =
    self.mapValues { r => Greater(r, d) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def localGreaterRightAssociative(d: Double) =
    self.mapValues { r => Greater(d, r) }

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    */
  def >(d: Double) = localGreater(d)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is greater than the input
    * double, else 0.
    *
    * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
    */
  def >>:(d: Double) = localGreaterRightAssociative(d)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the rasters are greater than the next
    * raster, else 0.
    */
  def localGreater(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Greater.apply)

  /**
    * Returns a TileLayerSeq with data of BitCellType, where cell values equal 1 if
    * the corresponding cell valued of the raster are greater than the next
    * raster, else 0.
    */
  def >(other: Seq[(K, Tile)]) = localGreater(other)
}
