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

package geotrellis.spark.io.hadoop

import geotrellis.spark._

import org.apache.spark.Partitioner

object KeyPartitioner {
  def apply[K: Ordering](splits: Array[K]): KeyPartitioner[K] = 
    new KeyPartitioner[K](splits)
}

class KeyPartitioner[K: Ordering](splits: Array[K]) extends Partitioner {
  private val ordering = implicitly[Ordering[K]]

  override def getPartition(key: Any): Int = 
    getPartition(key.asInstanceOf[K])

  def getPartition(key: K): Int = {
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

  def minKey(partition: Int): KeyBound[K] =
    if(partition == 0) MinKeyBound[K]()
    else ValueKeyBound(splits(partition))

  def maxKey(partition: Int): KeyBound[K] =
    if(partition == numPartitions - 1) MaxKeyBound[K]
    else ValueKeyBound(splits(partition))

  def intersects(partition: Int, minKey: K, maxKey: K) =
    getPartition(minKey) <= partition || partition <= getPartition(maxKey)

  override def hashCode: Int = splits.hashCode
  override def toString = {
    val s = if (splits.isEmpty) "Empty" else splits.zipWithIndex.mkString
    s"Partitioner split points: $s"
  }
}
