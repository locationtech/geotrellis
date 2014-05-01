package geotrellis.spark.formats
import geotrellis.spark.TestEnvironment
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.DataOutputStream
import java.io.DataInputStream
import org.apache.hadoop.io.Writable
import scala.reflect.ClassTag

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