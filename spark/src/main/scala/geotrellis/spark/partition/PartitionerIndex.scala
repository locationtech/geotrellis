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

package geotrellis.spark.partition

import geotrellis.tiling.{SpatialKey, SpaceTimeKey}
import geotrellis.spark._
import geotrellis.layers.index.KeyIndex
import geotrellis.layers.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}

/** Coarse KeyIndex to be used for partitioning of RDDs.
  * Coarseness means that multiple keys will be mapped to a single SFC value.
  * This many to one mapping forms spatially relate key blocks
  */
trait PartitionerIndex[K] extends Serializable {
  def toIndex(key: K): BigInt
  def indexRanges(keyRange: (K, K)): Seq[(BigInt, BigInt)]
}

object PartitionerIndex {

  /**
    * This is a reasonable default value. Operating on 512x512 tiles of Doubles
    * This partitioner will produces partitions of approximately half a gigabyte.
    */
  implicit object SpatialPartitioner extends  PartitionerIndex[SpatialKey] {
    private def toZ(key: SpatialKey): Z2 = Z2(key.col >> 4, key.row >> 4)

    def toIndex(key: SpatialKey): BigInt = toZ(key).z

    def indexRanges(keyRange: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] =
      Z2.zranges(toZ(keyRange._1), toZ(keyRange._2))
  }

  /**
    * This is hoped to be a reasonable default value.
    * The partitioner groups keys in 16x16 blocks spatially and by year temporally.
    */
  implicit object SpaceTimePartitioner extends  PartitionerIndex[SpaceTimeKey] {
    private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col >> 4, key.row >> 4, key.time.getYear)

    def toIndex(key: SpaceTimeKey): BigInt = toZ(key).z

    def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(BigInt, BigInt)] =
      Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
  }
}
