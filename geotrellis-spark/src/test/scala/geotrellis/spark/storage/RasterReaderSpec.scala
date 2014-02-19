package geotrellis.spark.storage
import geotrellis.spark.TestEnvironment
import geotrellis.spark.utils.SparkUtils
import org.scalatest.matchers.ShouldMatchers
import geotrellis.spark.formats.TileIdWritable
import org.apache.hadoop.fs.Path

class RasterReaderSpec extends TestEnvironment with ShouldMatchers {

  val allOnes = new Path(inputHome, "all-ones/10")

  private def read(start: Long, end: Long): Int = {
    val reader = RasterReader(allOnes, conf, TileIdWritable(start), TileIdWritable(end))
    val numEntries = reader.count(_ => true)
    reader.close()
    numEntries
  }

  describe("RasterReader") {
    it("should retrieve all entries") {
      read(208787, 211861) should be(12)
    }
    it("should retrieve all entries when no range is specified") {
      read(Long.MinValue, Long.MaxValue) should be(12)
    }
    it("should handle a non-existent start and end") {
      read(0, 209810) should be(3)
    }
    it("should be able to skip a partition") {
      read(210838, Long.MaxValue) should be(3)
    }
    it("should be handle start=end") {
      read(209811,209811) should be(1)
    }
  }
}