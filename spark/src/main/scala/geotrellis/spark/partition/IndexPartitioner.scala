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

import geotrellis.tiling.Bounds
import geotrellis.spark._
import geotrellis.layers.index._
import geotrellis.layers.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}
import geotrellis.util._

import org.apache.spark._
import org.apache.spark.rdd.{ShuffledRDD, RDD}

import scala.collection.mutable.ArrayBuffer
import scala.collection.Searching._
import scala.reflect._

/**
  * Uses KeyIndex to partition an RDD in memory, giving its records
  * some spatial locality.  When persisting an RDD partitioned by this
  * partitioner we can safely assume that all records contributing to
  * the same SFC index will reside in one partition.
  */
class IndexPartitioner[K](index: KeyIndex[K], count: Int) extends Partitioner {
  val breaks: Array[BigInt] =
    if (count > 1)
      KeyIndex.breaks(index.keyBounds, index, count - 1).sorted.toArray
    else
      Array(BigInt(-1)) // XXX

  def numPartitions = breaks.length + 1

  /**
    * Because breaks define divisions rather than breaks, all
    * indexable keys will have a bucket.  Keys that are far out of
    * bounds will be assigned to either first or last partition.
    */
  def getPartition(key: Any): Int = {
    val i = index.toIndex(key.asInstanceOf[K])

    breaks.search(i).insertionPoint // XXX requires Scala 2.11 or later
  }
}

object IndexPartitioner {
  def apply[K](index: KeyIndex[K], count: Int): IndexPartitioner[K] =
    new IndexPartitioner(index, count)

  def apply[K](bounds: Bounds[K], indexMethod: KeyIndexMethod[K], count: Int): IndexPartitioner[K] =
    new IndexPartitioner(indexMethod.createIndex(bounds.get), count)
}
