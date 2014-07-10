package geotrellis.spark.formats
import org.scalatest._

class TileIdWritableSpec extends FunSpec with Matchers with SerializerTester {
  describe("java serialization") {
    it("should serdes TileIdWritable") {
      val tw = TileIdWritable(1)
      val actualtw = testJavaSerialization(tw)
      actualtw should be(tw)
    }
  }
  describe("hadoop serialization") {
    it("should serdes TileIdWritable") {
      val tw = TileIdWritable(1)
      val actualtw = testHadoopSerialization(tw)
      actualtw should be(tw)
    }
  }
}