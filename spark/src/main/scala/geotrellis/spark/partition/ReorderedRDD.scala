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

import org.apache.spark._
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag


case class ReorderedPartition(index: Int, parentPartition: Option[Partition]) extends Partition

class ReorderedDependency[T](rdd: RDD[T], f: Int => Option[Int]) extends NarrowDependency[T](rdd) {
  def getParents(partitionId: Int): List[Int] = f(partitionId).toList
}

class ReorderedSpaceRDD[K, V](rdd: RDD[(K, V)], part: SpacePartitioner[K]) extends RDD[(K, V)](rdd.context, Nil) {
  val sourcePart = {
    val msg =  s"ReorderedSpaceRDD requires that $rdd has a SpacePartitioner[K]"
    require(rdd.partitioner.isDefined, msg)
    require(rdd.partitioner.get.isInstanceOf[SpacePartitioner[_]], msg)
    rdd.partitioner.get.asInstanceOf[SpacePartitioner[K]]
  }

  override val partitioner = Some(part)

  override def getDependencies: Seq[Dependency[_]] = {
    List(new ReorderedDependency(rdd, { i => sourcePart.regionIndex(part.regions(i)) }))
  }

  override def getPartitions = {
    for (index <- 0 until part.numPartitions) yield {
      val targetRegion = part.regions(index)
      val sourceRegion = sourcePart.regionIndex(targetRegion)
      new ReorderedPartition(index, for (i <- sourceRegion) yield rdd.partitions(i))
    }
  }.toArray

  override def compute(split: Partition, context: TaskContext) = {
    split.asInstanceOf[ReorderedPartition].parentPartition match {
      case Some(p) => rdd.iterator(p, context)
      case None => Iterator.empty
    }
  }
}
