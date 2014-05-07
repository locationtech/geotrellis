package geotrellis.spark.formats
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class TileIdZoomWritableSpec extends FunSpec with ShouldMatchers with SerializerTester {
  describe("java serialization") {
    it("should serdes TileIdZoomWritable") {
      val tzw = TileIdZoomWritable(1, 10)
      val actualtzw = testJavaSerialization(tzw)
      actualtzw should be(tzw)
    }
  }
  describe("hadoop serialization") {
    it("should serdes TileIdZoomWritable") {
      val tzw = TileIdZoomWritable(1, 10)
      val actualtzw = testHadoopSerialization(tzw)
      actualtzw should be(tzw)
    }
  }
}