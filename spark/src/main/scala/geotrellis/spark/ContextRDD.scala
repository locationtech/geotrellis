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

package geotrellis.spark

import geotrellis.layers.Metadata
import geotrellis.raster._
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

object ContextRDD {
  def apply[K, V, M](rdd: RDD[(K, V)], metadata: M): RDD[(K, V)] with Metadata[M] =
    new ContextRDD(rdd, metadata)

  implicit def tupleToContextRDD[K, V, M](tup: (RDD[(K, V)], M)): ContextRDD[K, V, M] =
    new ContextRDD(tup._1, tup._2)
}

class ContextRDD[K, V, M](val rdd: RDD[(K, V)], val metadata: M) extends RDD[(K, V)](rdd) with Metadata[M] {
  override val partitioner = rdd.partitioner

  def compute(split: Partition, context: TaskContext) =
    rdd.iterator(split, context)

  def getPartitions: Array[Partition] =
    rdd.partitions
}
