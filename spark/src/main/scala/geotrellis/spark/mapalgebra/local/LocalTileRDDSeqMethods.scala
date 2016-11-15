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
import geotrellis.raster.mapalgebra.local._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import scala.reflect._


abstract class LocalTileRDDSeqMethods[K: ClassTag] extends MethodExtensions[Traversable[RDD[(K, Tile)]]] {
  private def r(f: Traversable[Tile] => (Tile), partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self match {
      case Seq() => sys.error("raster rdd operations can't be applied to empty seq!")
      case Seq(rdd) => rdd
      case _ => self.head.combineValues(self.tail, partitioner)(f)
    }

  def localAdd: RDD[(K, Tile)] = localAdd(None)
  def localAdd(partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ Add.apply }, partitioner)

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety: RDD[(K, Tile)] = localVariety(None)
  def localVariety(partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ Variety.apply }, partitioner)

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean: RDD[(K, Tile)] = localMean(None)
  def localMean(partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ Mean.apply }, partitioner)

  def localMin: RDD[(K, Tile)] = localMin(None)
  def localMin(partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ Min.apply }, partitioner)

  def localMinN(n: Int): RDD[(K, Tile)] = localMinN(n, None)
  def localMinN(n: Int, partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ MinN(n, _) }, partitioner)

  def localMax: RDD[(K, Tile)] = localMax(None)
  def localMax(partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ Max.apply }, partitioner)

  def localMaxN(n: Int): RDD[(K, Tile)] = localMaxN(n, None)
  def localMaxN(n: Int, partitioner: Option[Partitioner]): RDD[(K, Tile)] = r ({ MaxN(n, _) }, partitioner)

  def localMinority(n: Int = 0, partitioner: Option[Partitioner] = None) = r ({ Minority(n, _) }, partitioner)

  def localMajority(n: Int = 0, partitioner: Option[Partitioner] = None) = r ({ Majority(n, _) }, partitioner)
}
