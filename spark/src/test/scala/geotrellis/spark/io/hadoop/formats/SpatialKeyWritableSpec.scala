package geotrellis.spark.io.hadoop.formats
import org.scalatest._

class SpatialKeyWritableSpec extends FunSpec with Matchers with SerializerTester {
  describe("java serialization") {
    it("should serdes SpatialKeyWritable") {
      val tw = SpatialKeyWritable(1->2)
      val actualtw = testJavaSerialization(tw)
      actualtw should be(tw)
    }
  }
  describe("hadoop serialization") {
    it("should serdes SpatialKeyWritable") {
      val tw = SpatialKeyWritable(1->2)
      val actualtw = testHadoopSerialization(tw)
      actualtw should be(tw)
    }
  }
}
