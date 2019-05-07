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
import geotrellis.layers.index.zcurve._
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.apache.spark.Partitioner
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.scalatest._

class ReorderedRDDSpec extends FunSpec with Matchers with TestEnvironment {
  import TestImplicits._

  val bounds1 = KeyBounds(SpatialKey(0,0), SpatialKey(10,10))
  val part1 = SpacePartitioner(bounds1)
  val rdd1: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 0 to 10
      row <- 0 to 10
    } yield (SpatialKey(col, row), col + row)
  }.partitionBy(part1)


  val bounds2 = KeyBounds(SpatialKey(5,5), SpatialKey(15,15))
  val part2 = SpacePartitioner(bounds2)
  val rdd2: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 5 to 15
      row <- 5 to 15
    } yield (SpatialKey(col, row), col + row)
  }.partitionBy(part2)

  it("should reorder partitions"){
    val res = new ReorderedSpaceRDD(rdd1, SpacePartitioner(bounds2))
    res.collect() should not be empty
  }

  it("should reorder to empty"){
    val res = new ReorderedSpaceRDD(rdd1, SpacePartitioner[SpatialKey](EmptyBounds))
    res.collect() shouldBe empty
  }

  val partEmpty = SpacePartitioner[SpatialKey](EmptyBounds)
  val rddEmpty = sc.emptyRDD[(SpatialKey, Int)].partitionBy(partEmpty)

  it("should reorder from empty"){
    val res = new ReorderedSpaceRDD(rddEmpty, part1)
    for (part <- res.getPartitions) {
      part.asInstanceOf[ReorderedPartition].parentPartition should be (None)
    }
    res.collect() shouldBe empty
  }
}
