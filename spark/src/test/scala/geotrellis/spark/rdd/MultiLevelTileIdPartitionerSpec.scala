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
import geotrellis.feature.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.formats.TileIdZoomWritable
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import java.awt.image.DataBuffer
import geotrellis.spark.formats.SerializerTester

class MultiLevelTileIdPartitionerSpec extends FunSpec with TestEnvironment with ShouldMatchers with SerializerTester {

  // maximum number of zoom levels for the pyramid in this test
  final val Levels = 3

  // create directories for each zoom level, and a PyramidMetadata
  def setup = {
    (1 to Levels).foreach(l => mkdir(new Path(outputLocal, l.toString)))

    val meta = PyramidMetadata(
      Extent(1, 1, 1, 1),
      512,
      1,
      Double.NaN,
      DataBuffer.TYPE_FLOAT,
      Levels,
      Map("1" -> new RasterMetadata(PixelExtent(0, 0, 0, 0), TileExtent(0, 0, 0, 0))))
    meta.save(outputLocal, conf)

  }

  setup

  // create a partition given split points for each level
  def getPartitioner(splits: Map[Int, Seq[Long]]) = {
    val splitGenerators = splits.map {
      case (level, splitSeq) =>
        (level, new SplitGenerator { def getSplits = splitSeq })
    }
    val pyramid = outputLocal
    MultiLevelTileIdPartitioner(splitGenerators, pyramid, conf)
  }

  /* 
   * Tests to make sure offsets are appropriately computed. This is done by 
   * setting single partition at every level  
   */
  describe("Offsets calculation - numPartitions = 1 for every level") {
    val partitioner = getPartitioner(Map(1 -> Seq(), 2 -> Seq(), 3 -> Seq()))

    it("should assign tileId 0 of level 3 to partition 0") {
      partitioner.getPartition(TileIdZoomWritable(0, 3)) should be(0)
    }
    it("should assign tileId 0 of level 2 to partition 1") {
      partitioner.getPartition(TileIdZoomWritable(0, 2)) should be(1)
    }
    it("should assign tileId 0 of level 1 to partition 2") {
      partitioner.getPartition(TileIdZoomWritable(0, 1)) should be(2)
    }
  }

  /* 
   * Tests on non-trivial number of partitions at multiple zoom levels,
   * level 3 gets 3 partitions, level 2 gets 2 partitions, and level 1 gets 1 partition. 
   * this covers all cases of keys on the split points, between split points, etc. for 
   * each of the three levels 
   */
  describe("getPartition on non-empty splits") {
    val partitioner = getPartitioner(Map(1 -> Seq(), 2 -> Seq(10), 3 -> Seq(10, 20)))

    it("should assign tileId 0 (before first split point) of level 3 to partition 0") {
      partitioner.getPartition(TileIdZoomWritable(0, 3)) should be(0)
    }

    it("should assign tileId 10 (first split point) of level 3 to partition 0") {
      partitioner.getPartition(TileIdZoomWritable(10, 3)) should be(0)
    }

    it("should assign tileId 11 of level 3 (between first and second split points) to partition 1") {
      partitioner.getPartition(TileIdZoomWritable(11, 3)) should be(1)
    }

    it("should assign tileId 20 of level 3 (second split point) to partition 1") {
      partitioner.getPartition(TileIdZoomWritable(20, 3)) should be(1)
    }

    it("should assign tileId 21 of level 3 (after second split point) to partition 2") {
      partitioner.getPartition(TileIdZoomWritable(21, 3)) should be(2)
    }

    it("should assign tileId 0 (before first split point) of level 2 to partition 3") {
      partitioner.getPartition(TileIdZoomWritable(0, 2)) should be(3)
    }

    it("should assign tileId 10 (split point) of level 2 to partition 3") {
      partitioner.getPartition(TileIdZoomWritable(10, 2)) should be(3)
    }

    it("should assign tileId 21 (greater than max split point) of level 2 to partition 4") {
      partitioner.getPartition(TileIdZoomWritable(21, 2)) should be(4)
    }

    it("should assign tileId 0 of level 1 to partition 5") {
      partitioner.getPartition(TileIdZoomWritable(0, 1)) should be(5)
    }
  }

  describe("java serialization") {
    it("should serdes MultiLevelTileIdPartitioner") {
      val expectedP = getPartitioner(Map(1 -> Seq(10), 2 -> Seq(10,20), 3 -> Seq(10,20,30)))
      val actualP = testJavaSerialization(expectedP)      
      actualP should be(expectedP)
    }
  }
}