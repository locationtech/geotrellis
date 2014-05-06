package geotrellis.spark.formats
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MultiLevelTileIdWritableSpec extends FunSpec with ShouldMatchers with SerializerTester {
  describe("java serialization") {
    it("should serdes MultiLevelTileIdWritable") {
      val mltw = MultiLevelTileIdWritable(1, 10, 100)
      val actualmltw = testJavaSerialization(mltw)
      actualmltw should be(mltw)
    }
  }
  describe("hadoop serialization") {
    it("should serdes MultiLevelTileIdWritable") {
      val mltw = MultiLevelTileIdWritable(1, 10, 100)
      val actualmltw = testHadoopSerialization(mltw)
      actualmltw should be(mltw)
    }
  }
}