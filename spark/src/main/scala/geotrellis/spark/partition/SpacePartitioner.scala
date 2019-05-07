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

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve.{Z3, Z2, ZSpatialKeyIndex}
import geotrellis.util._

import org.apache.spark._
import org.apache.spark.rdd.{ShuffledRDD, RDD}

import scala.collection.mutable.ArrayBuffer
import scala.reflect._

case class SpacePartitioner[K: Boundable: ClassTag](bounds: Bounds[K])
  (implicit index: PartitionerIndex[K]) extends Partitioner {

  val regions: Array[BigInt] =
    bounds match {
      case b: KeyBounds[K] =>
        for {
          (start, end) <- index.indexRanges(b).toArray
          p <- start to end
        } yield p

      case EmptyBounds =>
        Array.empty
    }

  def numPartitions = regions.length

  def getPartition(key: Any): Int = {
    val region = index.toIndex(key.asInstanceOf[K])
    val regionIndex = regions.indexOf(region)
    if (regionIndex > -1) regionIndex
    else (region % numPartitions).toInt // overflow for keys, at this point this should no longer be considered spatially partitioned
  }

  def containsKey(key: Any): Boolean = {
    val i = index.toIndex(key.asInstanceOf[K])
    regions.indexOf(i) > -1
  }

  def regionIndex(region: BigInt): Option[Int] = {
    // Note: Consider future design where region can overlap several partitions, would change Option -> List
    val i = regions.indexOf(region)
    if (i > -1) Some(i) else None
  }

  /**
    * Use this partitioner as a partitioner for rdd.
    * The rdd may have a SpacePartitioner already.
    * If it is in sync with Bounds in the Metadata we assume it to be valid .
    * Otherwise we assume it has degraded to be a hash partitioner and we must perform a shuffle.
    */
  def apply[V: ClassTag, M: GetComponent[?, Bounds[K]]](rdd: RDD[(K, V)] with Metadata[M]): RDD[(K, V)] with Metadata[Bounds[K]] = {
    val kb: Bounds[K] = rdd.metadata.getComponent[Bounds[K]]
    rdd.partitioner match {
      case Some(part: SpacePartitioner[K]) if part.bounds == kb =>
        ContextRDD(
          new ReorderedSpaceRDD(rdd.filter(r => containsKey(r._1)), this),
          bounds)

      case _ =>
        ContextRDD(
          new ShuffledRDD[K, V, V](rdd.filter(r => containsKey(r._1)), this).asInstanceOf[RDD[(K, V)]],
          bounds)
    }
  }
}
