package geotrellis.spark.rdd

import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.utils.SparkUtils

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import org.apache.hadoop.fs.Path

class TileIdPartitionerSpec extends FunSpec with MustMatchers with ShouldMatchers {

  val conf = SparkUtils.createHadoopConfiguration

  def getPartitioner(seq: Seq[Long]) = {
    val splitGenerator = new SplitGenerator {
      def getSplits = seq
    }
    val imagePath = new Path(java.nio.file.Files.createTempDirectory("splits").toUri())
    TileIdPartitioner(splitGenerator, imagePath, conf)
  }

  /* tests to see if we have two partitions - 10 and 20, then keys 1, 10, 11, 20, 21 are 
   * assigned to partitions 0, 0, 1, 1, and 2 this covers all cases of keys on the split 
   * points, between split points, etc. 
   */
  describe("getPartition on non-empty partitioner") {

    val partitioner = getPartitioner(Seq(10, 20))

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
}