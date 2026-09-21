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

import org.apache.spark.*
import org.apache.spark.rdd.*



case class ReorderedPartition(index: Int, parentPartition: Option[Partition]) extends PartitionBase with Partition {
  // Spark's `Partition` implements `equals` via `super.equals`, which Scala 3 cannot mix in.
  // Spell out the structural equality the case class would otherwise synthesise.
  override def equals(other: Any): Boolean = other match {
    case that: ReorderedPartition => index == that.index && parentPartition == that.parentPartition
    case _ => false
  }
  override def hashCode(): Int = (index, parentPartition).##
}

class ReorderedDependency[T](rdd: RDD[T], f: Int => Option[Int]) extends NarrowDependency[T](rdd) {
  def getParents(partitionId: Int): List[Int] = f(partitionId).toList
}

class ReorderedSpaceRDD[K, V](rdd: RDD[(K, V)], part: SpacePartitioner[K]) extends RDD[(K, V)](rdd.context, Nil) {
  val sourcePart = {
    val msg =  s"ReorderedSpaceRDD requires that $rdd has a SpacePartitioner[K] with same indexing"
    require(rdd.partitioner.isDefined, msg)
    require(rdd.partitioner.get.isInstanceOf[SpacePartitioner[?]], msg)
    require(rdd.partitioner.get.asInstanceOf[SpacePartitioner[K]].hasSameIndex(part), msg)
    rdd.partitioner.get.asInstanceOf[SpacePartitioner[K]]
  }

  override val partitioner: Some[SpacePartitioner[K]] = Some(part)

  private def getSourcePartitionId(targetPartitionId: Int): Option[Int] = {
    sourcePart.regionIndex(part.regions(targetPartitionId))
  }

  override def getDependencies: Seq[Dependency[?]] = {
    List(new ReorderedDependency(rdd, { i => getSourcePartitionId(i) }))
  }

  override def getPartitions: Array[Partition]= {
    for (index <- 0 until part.numPartitions) yield {
      new ReorderedPartition(index, for (i <- getSourcePartitionId(index)) yield rdd.partitions(i))
    }
  }.toArray

  override def compute(split: Partition, context: TaskContext) = {
    split.asInstanceOf[ReorderedPartition].parentPartition match {
      case Some(p) => rdd.iterator(p, context)
      case None => Iterator.empty
    }
  }
}
