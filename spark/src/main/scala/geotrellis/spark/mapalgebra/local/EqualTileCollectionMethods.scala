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
import geotrellis.raster.mapalgebra.local.Equal
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.util.MethodExtensions

trait EqualTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * integer, else 0.
    */
  def localEqual(i: Int) =
    self.mapValues { r => Equal(r, i) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the input
    * double, else 0.
    */
  def localEqual(d: Double) =
    self.mapValues { r => Equal(r, d) }

  /**
    * Returns a Tile with data of BitCellType, where cell values equal 1 if
    * the corresponding cell value of the input raster is equal to the provided
    * raster, else 0.
    */
  def localEqual(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Equal.apply)
}
