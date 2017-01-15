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

package geotrellis.spark.streaming.merge

import geotrellis.raster.merge._
import geotrellis.spark.merge.TileRDDMerge
import geotrellis.util.MethodExtensions
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

class TileDStreamMergeMethods[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](val self: DStream[(K, V)]) extends MethodExtensions[DStream[(K, V)]] {
  def merge(other: DStream[(K, V)]): DStream[(K, V)] =
    self.transformWith(other, (selfRdd: RDD[(K, V)], otherRdd: RDD[(K, V)]) => TileRDDMerge(selfRdd, otherRdd))

  def merge(): DStream[(K, V)] =
    self.transform(TileRDDMerge(_, None))

  def merge(partitioner: Option[Partitioner]): DStream[(K, V)] =
    self.transform(TileRDDMerge(_, partitioner))
}
