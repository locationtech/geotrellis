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
import geotrellis.spark.{TileIdPartitioner, TestEnvironment}
import org.scalatest._
import geotrellis.spark.io.hadoop.formats.SerializerTester

class TileIdPartitionerSpec extends FunSpec with TestEnvironment with Matchers with SerializerTester {
  def getPartitioner(seq: Traversable[Long]) = {
    TileIdPartitioner(seq.toArray)
  }

  /* tests to see if we have two partitions - 10 and 20, then keys 1, 10, 11, 20, 21 are 
   * assigned to partitions 0, 0, 1, 1, and 2 this covers all cases of keys on the split 
   * points, between split points, etc. 
   */
  describe("getPartition on non-empty partitioner") {

    val partitioner = getPartitioner(Seq(10L, 20L))

    it("should assign tileId Long.MinValue to partition 0") {
      partitioner.getPartition(Long.MinValue) should be(0)
    }

    it("should assign tileId Long.MaxValue to partition 2") {
      partitioner.getPartition(Long.MaxValue) should be(2)
    }

    it("should assign tileId 0 (minimum key) to partition 0") {
      partitioner.getPartition(0L) should be(0)
    }

    it("should assign tileId 10 (first split point) to partition 0") {
      partitioner.getPartition(10L) should be(0)
    }

    it("should assign tileId 11 (between first and second split points) to partition 1") {
      partitioner.getPartition(11L) should be(1)
    }

    it("should assign tileId 20 (second split point) to partition 1") {
      partitioner.getPartition(20L) should be(1)
    }

    it("should assign tileId 21 (greater than maximum split point) to partition 2") {
      partitioner.getPartition(21L) should be(2)
    }
  }

  describe("getPartition on empty partitioner") {
    val partitioner = getPartitioner(Seq())
    it("should assign all tiles to partition 0") {
      partitioner.getPartition(0L) should be(0)
      partitioner.getPartition(Long.MaxValue) should be(0)
    }
  }

  /* range tests - if we have three partitions (0, 1, 2) corresponding to the two split points 
   * 10 and 20, then these are the valid ranges for those partitions
   */
  describe("range") {
    val partitioner = getPartitioner(Seq(10L, 20L))

    it("should handle range of partition 0") {
      partitioner.range(0) should be(Long.MinValue, 10L)
    }
    it("should handle range of partition 1") {
      partitioner.range(1) should be(11L, 20L)
    }
    it("should handle range of partition 2") {
      partitioner.range(2) should be(21L, Long.MaxValue)
    }
  }

  describe("splitGenerator") {
    val expectedSplits = Seq(10L)
    val partitioner = getPartitioner(expectedSplits)

    it("should give back the correct splits") {
      partitioner.splits should be(expectedSplits)
    }
  }

  describe("java serialization") {
    it("should serdes TileIdPartitioner") {
      val expectedP = getPartitioner(Seq(10L))
      val actualP = testJavaSerialization(expectedP)
      actualP.splits.toSeq should be(expectedP.splits.toSeq)
    }
  }
}
