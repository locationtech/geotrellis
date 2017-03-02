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

package geotrellis.spark.mapalgebra

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

abstract class CombineMethods[K: ClassTag, V: ClassTag] extends MethodExtensions[RDD[(K, V)]] {
  def combineValues[R: ClassTag](other: RDD[(K, V)])(f: (V, V) => R): RDD[(K, R)] = combineValues(other, None)(f)
  def combineValues[R: ClassTag](other: RDD[(K, V)], partitioner: Option[Partitioner])(f: (V, V) => R): RDD[(K, R)] =
    partitioner
      .fold(self.join(other))(self.join(other, _))
      .mapValues { case (tile1, tile2) => f(tile1, tile2) }

  def combineValues[R: ClassTag](others: Traversable[RDD[(K, V)]])(f: Iterable[V] => R): RDD[(K, R)] = combineValues(others, None)(f)
  def combineValues[R: ClassTag](others: Traversable[RDD[(K, V)]], partitioner: Option[Partitioner])(f: Iterable[V] => R): RDD[(K, R)] = {
    val union = self.sparkContext.union(self :: others.toList)
    partitioner
      .fold(union.groupByKey(Partitioner.defaultPartitioner(self, others.toSeq: _*)))(union.groupByKey(_))
      .mapValues { case tiles => f(tiles) }
  }
}
