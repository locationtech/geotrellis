/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.spark

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

/**
 * An RDD where keys represent a point in N-Dimensional space.
 * Currently this does not hold the KeyBounds value. For the moment it's purpose is to provider a superclass for
 * RasterRDD where the payload is generic.
 *
 * @param rdd       RDD of keys and values in n-dimensional space
 * @param bounds    TODO: n-dimensional bounding box containing all the points in this RDD
 */
class BoundRDD[K: ClassTag, V: ClassTag](rdd: RDD[(K, V)]) extends RDD[(K, V)](rdd) {
  val defaultPartitionSize: Int =  16384

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, V)].iterator(split, context)

  override def getPartitions: Array[Partition] =
    firstParent[(K, V)].partitions

}
