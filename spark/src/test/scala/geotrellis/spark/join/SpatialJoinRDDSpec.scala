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

package geotrellis.spark.join

import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.spark.testkit._

import org.apache.spark.Partitioner
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.scalatest._
import spire.math.interval.EmptyBound

class SpatialJoinRDDSpec extends FunSpec with Matchers with TestEnvironment {
  // Import the PartitionerIndex that we will be using for the tests.
  import geotrellis.spark.partition.TestImplicits._

  val bounds1 = KeyBounds(SpatialKey(0,0), SpatialKey(10,10))
  val part1 = SpacePartitioner(bounds1)
  val rdd1: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 0 to 10
      row <- 0 to 10
    } yield (SpatialKey(col, row), col + row)
  }
  val pr1 = ContextRDD(rdd1, part1.bounds)


  val bounds2 = KeyBounds(SpatialKey(5,5), SpatialKey(15,15))
  val part2 = SpacePartitioner(bounds2)
  val rdd2: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 5 to 15
      row <- 5 to 15
    } yield (SpatialKey(col, row), col + row)
  }
  val pr2 = ContextRDD(rdd2, part2.bounds)


  val bounds3 = KeyBounds(SpatialKey(20,20), SpatialKey(25,25))
  val rdd3: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 20 to 25
      row <- 20 to 25
    } yield (SpatialKey(col, row), col + row)
  }
  val pr3 = ContextRDD(rdd3, bounds3: Bounds[SpatialKey])

   val rddEmpty = sc.emptyRDD[(SpatialKey, Int)]
   val prEmpty = ContextRDD(rddEmpty, SpacePartitioner[SpatialKey](EmptyBounds))

  def maxPartitionSize(rdd: RDD[_]): Int = {
    rdd.mapPartitions(it => Iterator(it.size)).collect().max
  }

  it("find default partitioner"){
    info(pr1.partitioner.toString)
    info(pr2.partitioner.toString)
    info(Partitioner.defaultPartitioner(pr1, pr2).toString)
  }

  it("left join correctly") {
    val res = pr1.spatialLeftOuterJoin(pr2)
    val expected = new PairRDDFunctions(rdd1).leftOuterJoin(pr2)

    info(s"PairRDDFunctions partitioner: ${expected.partitioner}")
    info(s"SpaceRDD join partitioner: ${res.partitioner}")
    info(s"  number of partitions: ${res.partitions.length}")
    res.partitioner.get should be (part1)
    res.collect() sameElements expected.collect()
    maxPartitionSize(res) should be <= 4
  }

  it("left join with non intersecting rdds") {
    val res = pr1.leftOuterJoin(pr3)
    val expected = pr1.mapValues(v => (v, None))

    res.collect() sameElements expected.collect()
  }

   it("left join to empty SpaceRDD") {
     val res = prEmpty.leftOuterJoin(pr3)
     val records = res.collect()
     records sameElements Array[(SpatialKey, Int)]()
     info("records: " + records.length)
   }

  it("inner join correctly") {
    val res = pr1.spatialJoin(pr2)
    val expected = new PairRDDFunctions(rdd1).join(pr2)

    info(s"PairRDDFunctions partitioner: ${expected.partitioner}")
    info(s"SpaceRDD join partitioner: ${res.partitioner}")
    res.partitioner.get should be equals SpacePartitioner(KeyBounds(SpatialKey(5,5), SpatialKey(10,10)))
    res.collect() sameElements expected.collect()
    maxPartitionSize(res) should be <= 4
  }

  it("inner join non intersecting rdds") {
    val res = pr1.join(pr3)
    val records =res.collect()
    records sameElements Array[(SpatialKey, Int)]()
    info("records: " + records.length)
  }

}
