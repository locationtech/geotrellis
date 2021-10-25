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

import geotrellis.layer.{SpatialKey, KeyBounds}
import geotrellis.store.index.zcurve._

object TestImplicits {
  implicit object TestPartitioner extends PartitionerIndex[SpatialKey] {
    private val zCurveIndex = new ZSpatialKeyIndex(KeyBounds(SpatialKey(0, 0), SpatialKey(100, 100)))

    def rescale(key: SpatialKey): SpatialKey =
      SpatialKey(key.col/2, key.row/2)

    override def toIndex(key: SpatialKey): BigInt =
      zCurveIndex.toIndex(rescale(key))

    override def indexRanges(r: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] =
      zCurveIndex.indexRanges((rescale(r._1), rescale(r._2)))
  }
}


object CustomPartitioning {

  implicit object AnotherPartitionerIndex extends PartitionerIndex[SpatialKey] {
    private val zCurveIndex = new ZSpatialKeyIndex(KeyBounds(SpatialKey(0, 0), SpatialKey(100, 100)))

    def rescale(key: SpatialKey): SpatialKey =
      SpatialKey(10 + key.col / 3, 10 + key.row / 3)

    override def toIndex(key: SpatialKey): BigInt =
      zCurveIndex.toIndex(rescale(key))

    override def indexRanges(r: (SpatialKey, SpatialKey)): Seq[(BigInt, BigInt)] =
      zCurveIndex.indexRanges((rescale(r._1), rescale(r._2)))
  }

  def getCustomSpacePartitioner(bounds: KeyBounds[SpatialKey]): SpacePartitioner[SpatialKey] = {
    // This space partitioner will use AnotherPartitionerIndex implicitly
    SpacePartitioner(bounds)
  }

}