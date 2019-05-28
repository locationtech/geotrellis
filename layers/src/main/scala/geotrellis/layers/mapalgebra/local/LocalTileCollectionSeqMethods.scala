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

package geotrellis.layers.mapalgebra.local

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.layers._
import geotrellis.util.MethodExtensions


abstract class LocalTileCollectionSeqMethods[K] extends MethodExtensions[Traversable[Seq[(K, Tile)]]] {
  private def r(f: Traversable[Tile] => (Tile)): Seq[(K, Tile)] =
    self match {
      case Seq() => sys.error("raster seq operations can't be applied to empty seq!")
      case Seq(rdd) => rdd
      case _ => self.head.combineValues(self.tail)(f)
    }

  def localAdd: Seq[(K, Tile)] = r { Add.apply }

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety: Seq[(K, Tile)] = r { Variety.apply }

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean: Seq[(K, Tile)] = r { Mean.apply }

  def localMin: Seq[(K, Tile)] = r { Min.apply }

  def localMinN(n: Int): Seq[(K, Tile)] = r { MinN(n, _) }

  def localMax: Seq[(K, Tile)] = r { Max.apply }

  def localMaxN(n: Int): Seq[(K, Tile)] = r { MaxN(n, _) }

  def localMinority(n: Int = 0) = r { Minority(n, _) }

  def localMajority(n: Int = 0) = r { Majority(n, _) }
}
