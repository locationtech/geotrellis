package geotrellis.spark.formats
import org.scalatest._

class TileIdZoomWritableSpec extends FunSpec with Matchers with SerializerTester {
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