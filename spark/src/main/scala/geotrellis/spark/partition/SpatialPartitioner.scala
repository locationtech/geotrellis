/*
 * Copyright 2019 Azavea
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

package geotrellis.spark.partition

import geotrellis.layer.{SpatialComponent, SpatialKey}
import geotrellis.store.index.zcurve._
import geotrellis.util._

import org.apache.spark._

class SpatialPartitioner[K: SpatialComponent](partitions: Int, bits: Int) extends Partitioner {
  def numPartitions: Int = partitions

  def getBits: Int = bits

  def getPartition(key: Any): Int = {
    val k = key.asInstanceOf[K]
    val SpatialKey(col, row) = k.getComponent[SpatialKey]
    ((Z2(col, row).z >> bits) % partitions).toInt
  }
}

object SpatialPartitioner {
  def apply[K: SpatialComponent](partitions: Int, bits: Int): SpatialPartitioner[K] =
    new SpatialPartitioner[K](partitions, bits)

  def apply[K: SpatialComponent](partitions: Int): SpatialPartitioner[K] =
    new SpatialPartitioner[K](partitions, 8)
}
