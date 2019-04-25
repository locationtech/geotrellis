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

package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.tiling.SpatialComponent
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

abstract class CollectNeighborsMethods[K: SpatialComponent: ClassTag, V](val self: RDD[(K, V)])
    extends MethodExtensions[RDD[(K, V)]] {
  def collectNeighbors(): RDD[(K, Iterable[(Direction, (K, V))])] =
    CollectNeighbors(self)
}
