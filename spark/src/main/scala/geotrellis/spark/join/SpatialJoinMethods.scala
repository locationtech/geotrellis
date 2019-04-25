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

package geotrellis.spark.join

import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.partition._
import org.apache.spark.rdd._
import geotrellis.util._

import scala.reflect._

abstract class SpatialJoinMethods[
  K: Boundable: PartitionerIndex: ClassTag,
  V: ClassTag,
  M: GetComponent[?, Bounds[K]]
] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def spatialLeftOuterJoin[W: ClassTag, M1: Component[?, Bounds[K]]](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, Option[W]))] with Metadata[Bounds[K]] =
    SpatialJoin.leftOuterJoin(self, right)

  def spatialJoin[W: ClassTag, M1: Component[?, Bounds[K]]](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, W))] with Metadata[Bounds[K]] =
    SpatialJoin.join(self, right)
}
