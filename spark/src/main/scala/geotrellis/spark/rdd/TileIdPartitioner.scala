/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.rdd

import geotrellis.spark._

import org.apache.spark.Partitioner

case class TileIdPartitioner(splits: Array[Long]) extends Partitioner {
  override def getPartition(key: Any): Int = {
    val index = java.util.Arrays.binarySearch(splits, key.asInstanceOf[Long])
    if (index < 0)
      (index + 1) * -1
    else
      index
  }

  override def numPartitions = splits.length + 1

  def range(partition: Int): (TileId, TileId) = {
    val min = if (partition == 0) Long.MinValue else splits(partition - 1) + 1
    val max = if (partition == splits.length) Long.MaxValue else splits(partition)
    (min, max)
  }

  override def hashCode: Int = splits.hashCode
  override def toString = "TileIdPartitioner split points: " + {
    if (splits.isEmpty) "Empty" else splits.zipWithIndex.mkString
  }
}
