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

package geotrellis.layers.hadoop

import org.apache.spark.Partitioner


object KeyPartitioner {
  def apply[K: Ordering](splits: Array[K]): KeyPartitioner[K] = 
    new KeyPartitioner[K](splits)
}

class KeyPartitioner[K: Ordering](splits: Array[K]) extends Partitioner {
  private val ordering = implicitly[Ordering[K]]

  override def getPartition(key: Any): Int = 
    getPartitionForKey(key.asInstanceOf[K])

  def getPartitionForKey(key: K): Int = {
    var len = numPartitions - 1
    var p = 0
    while (len > 0) {
      val half = len >>> 1
      val middle = p + half
      if(ordering.lt(key, splits(middle))) {
        len = half
      } else {
        p = middle + 1
        len = len - half - 1
      }
    }

    p
  }

  override def numPartitions = splits.length + 1

  def contains(partition: Int, key: K): Boolean =
    getPartition(key) == partition

  override def hashCode: Int = splits.hashCode
  override def toString = {
    val s = if (splits.isEmpty) "Empty" else splits.zipWithIndex.mkString
    s"Partitioner split points: $s"
  }
}
