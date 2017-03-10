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

package geotrellis.spark.sql.mapalgebra.local

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.spark.sql._
import geotrellis.util.MethodExtensions
import org.apache.spark.sql.Dataset

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

abstract class LocalTileDatasetSeqMethods[K <: Product: TypeTag: ClassTag] extends MethodExtensions[Traversable[Dataset[(K, Tile)]]] {

  private def r(f: Traversable[Tile] => (Tile)): Dataset[(K, Tile)] =
    self match {
      case Seq() => sys.error("raster rdd operations can't be applied to empty seq!")
      case Seq(ds) => ds
      case _ => self.head.combineValues(self.tail)(f)
    }

  def localAdd: Dataset[(K, Tile)] = r ({ Add.apply })

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety: Dataset[(K, Tile)] = r ({ Variety.apply })

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean: Dataset[(K, Tile)] = r ({ Mean.apply })

  def localMin: Dataset[(K, Tile)] = r ({ Min.apply })

  def localMinN(n: Int): Dataset[(K, Tile)] = r ({ MinN(n, _) })

  def localMax: Dataset[(K, Tile)] = r ({ Max.apply })

  def localMaxN(n: Int): Dataset[(K, Tile)] = r ({ MaxN(n, _) })

  def localMinority(n: Int = 0) = r ({ Minority(n, _) })

  def localMajority(n: Int = 0) = r ({ Majority(n, _) })
}
