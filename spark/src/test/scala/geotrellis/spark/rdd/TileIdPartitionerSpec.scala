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

package geotrellis.spark.rdd
import geotrellis.spark.TestEnvironment
import geotrellis.spark.formats.TileIdWritable

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class TileIdPartitionerSpec extends FunSpec with TestEnvironment with ShouldMatchers {

  def getPartitioner(seq: Seq[Long]) = {
    val splitGenerator = new SplitGenerator {
      def getSplits = seq
    }
    val pyramid = outputLocal
    TileIdPartitioner(splitGenerator, pyramid, conf)
  }

  /* tests to see if we have two partitions - 10 and 20, then keys 1, 10, 11, 20, 21 are 
   * assigned to partitions 0, 0, 1, 1, and 2 this covers all cases of keys on the split 
   * points, between split points, etc. 
   */
  describe("getPartition on non-empty partitioner") {

    val partitioner = getPartitioner(Seq(10, 20))

    it("should assign tileId Long.MinValue to partition 0") {
      partitioner.getPartition(TileIdWritable(Long.MinValue)) should be(0)
    }

    it("should assign tileId Long.MaxValue to partition 2") {
      partitioner.getPartition(TileIdWritable(Long.MaxValue)) should be(2)
    }

    it("should assign tileId 0 (minimum key) to partition 0") {
      partitioner.getPartition(TileIdWritable(0)) should be(0)
    }

    it("should assign tileId 10 (first split point) to partition 0") {
      partitioner.getPartition(TileIdWritable(10)) should be(0)
    }

    it("should assign tileId 11 (between first and second split points) to partition 1") {
      partitioner.getPartition(TileIdWritable(11)) should be(1)
    }

    it("should assign tileId 20 (second split point) to partition 1") {
      partitioner.getPartition(TileIdWritable(20)) should be(1)
    }

    it("should assign tileId 21 (greater than maximum split point) to partition 2") {
      partitioner.getPartition(TileIdWritable(21)) should be(2)
    }
  }

  describe("getPartition on empty partitioner") {
    val partitioner = getPartitioner(Seq())
    it("should assign all tiles to partition 0") {
      partitioner.getPartition(TileIdWritable(0)) should be(0)
      partitioner.getPartition(TileIdWritable(Long.MaxValue)) should be(0)
    }
  }

  /* range tests - if we have three partitions (0, 1, 2) corresponding to the two split points 
   * 10 and 20, then these are the valid ranges for those partitions
   */
  describe("range") {
    val partitioner = getPartitioner(Seq(10, 20))

    it("should handle range of partition 0") {
      partitioner.range(0) should be(TileIdWritable(Long.MinValue), TileIdWritable(10))
    }
    it("should handle range of partition 1") {
      partitioner.range(1) should be(TileIdWritable(11), TileIdWritable(20))
    }
    it("should handle range of partition 2") {
      partitioner.range(2) should be(TileIdWritable(21), TileIdWritable(Long.MaxValue))
    }
  }
}