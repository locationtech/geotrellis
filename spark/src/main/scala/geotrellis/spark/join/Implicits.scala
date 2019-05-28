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

import geotrellis.layers.Metadata
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd._

import scala.reflect._


object Implicits extends Implicits

trait Implicits {
  implicit class withSpatialJoinMethods[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]]
  ](val self: RDD[(K, V)] with Metadata[M])
    extends SpatialJoinMethods[K, V, M]

  implicit class withVectorJoinMethods[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ](val self: RDD[L]) extends VectorJoinMethods[L, R]
}
