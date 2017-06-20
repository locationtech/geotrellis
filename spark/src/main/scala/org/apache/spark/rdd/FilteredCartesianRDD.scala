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

/**
  * Adapted from [1] by Azavea, Inc.
  *
  * 1. https://github.com/apache/spark/blob/2f8776ccad532fbed17381ff97d302007918b8d8/core/src/main/scala/org/apache/spark/rdd/CartesianRDD.scala
  */
package org.apache.spark.rdd

import java.io.{IOException, ObjectOutputStream}

import scala.reflect.ClassTag

import org.apache.spark._
import org.apache.spark.util.Utils

/** Performs a cartesian join of two RDDs using filter and refine pattern.
  *
  * During RDD declaration n*m partitions will be generated, one for each possible cartesian mapping.
  * During RDD execution summary functions will be applied in a map-side reduce to `rrd1` and `rdd2`.
  * These results will be collected and filtered using `metapred` for partitions with potential matches.
  * Partition pairings with possible matches will be checked using `pred` in a refinement step.
  *
  * No shuffle from `rdd1` or `rdd2` will be performed by the filter step,
  * but the records of metardds, produced using the summary functions, will be shuffled (as they must be).
  * The metardds contain one item per partition (ex: a "bounding box" of records in parent rdd),
  * so it is assumed that this shuffle will be low cost.
  *
  * For efficient execution it is assumed that potential matches exist for limited number
  * of cartesian pairings, if no filtering is possible worst case scenario is full cartesian product.
  *
  * @param sc       SparkContext
  * @param pred     refinement predicate
  * @param metapred filter predicate
  * @param rdd1     RDD of elements on the left side of the cartisian join
  * @summaryFn1     summary function for rdd1 used to generate input for filter predicate
  * @param rdd2     RDD of elements on the right side of the cartisian join
  * @summaryFn2     summary function for rdd2 used to generate input for filter predicate
  */
sealed class FilteredCartesianRDD[T: ClassTag, U: ClassTag, V: ClassTag](
  sc: SparkContext,
  pred: (T, U) => Boolean,
  metapred: (V, V) => Boolean,
  var rdd1 : RDD[T],
  summaryFn1: Iterator[T] => Iterator[V],
  var rdd2 : RDD[U],
  summaryFn2: Iterator[U] => Iterator[V]
) extends RDD[(T, U)](sc, Nil) with Serializable {

  val numPartitionsInRdd2 = rdd2.partitions.length
  val metardd1 = rdd1.mapPartitions(summaryFn1, preservesPartitioning = true)
  val metardd2 = rdd2.mapPartitions(summaryFn2, preservesPartitioning = true)

  override def getPartitions: Array[Partition] = {
    // create the cross product split
    val array = new Array[Partition](rdd1.partitions.length * rdd2.partitions.length)
    for (s1 <- rdd1.partitions; s2 <- rdd2.partitions) {
      val idx = s1.index * numPartitionsInRdd2 + s2.index
      array(idx) = new CartesianPartition(idx, rdd1, rdd2, s1.index, s2.index)
    }
    array
  }

  override def getPreferredLocations(split: Partition): Seq[String] = {
    val currSplit = split.asInstanceOf[CartesianPartition]
    (rdd1.preferredLocations(currSplit.s1) ++ rdd2.preferredLocations(currSplit.s2)).distinct
  }

  override def compute(split: Partition, context: TaskContext): Iterator[(T, U)] = {
    val currSplit = split.asInstanceOf[CartesianPartition]
    val part1 = currSplit.s1 // partition for (|meta)rdd1
    val part2 = currSplit.s2 // partition for (|meta)rdd2
    require(metardd1.iterator(part1, context).hasNext, "Meta-RDD Failure")
    require(metardd2.iterator(part2, context).hasNext, "Meta-RDD Failure")

    /* A call to RDD.iterator invokes the block manager to compute and fetch
     * required partition of metardd1 and metardd2. Because subsequent calls
     * to RDD.iterator are happening within the same TaskContext the partitions
     * of metardds will only be computed once per task execution.
     * This is essentially a partition-wise .collect it may cause network traffic.
     */
    val meta1 = metardd1.iterator(part1, context).next
    val meta2 = metardd2.iterator(part2, context).next

    /* Information gathered from the metardd partitions is used
     * to determine if the RDD.iterator methods of parents should be invoked.
     * Returning an empty iterator here will short-circuit any IO for this partition.
     */
    if (metapred(meta1, meta2)) {
      for (x <- rdd1.iterator(part1, context);
           y <- rdd2.iterator(part2, context);
           if (pred(x,y))) yield (x, y)
    }
    else Iterator.empty
  }

  override def getDependencies: Seq[Dependency[_]] = List(
    new NarrowDependency(rdd1) {
      def getParents(id: Int): Seq[Int] = List(id / numPartitionsInRdd2)
    },
    new NarrowDependency(rdd2) {
      def getParents(id: Int): Seq[Int] = List(id % numPartitionsInRdd2)
    }
  )

  override def clearDependencies() {
    super.clearDependencies()
    rdd1 = null
    rdd2 = null
  }
}
