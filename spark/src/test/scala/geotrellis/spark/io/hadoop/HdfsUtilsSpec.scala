package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.spark._
import org.scalatest._
import org.apache.hadoop.fs.Path

class HdfsUtilsSpec extends FunSpec
    with Matchers
    with TestEnvironment
{
  describe("HdfsUtils") {
    it("should not crash with unuseful error messages when no files match listFiles") {
      val path = new Path("/this/does/not/exist") // Would be really weird if this did.
      an[IOException] should be thrownBy { HdfsUtils.listFiles(path, conf) }
    }
  }
}
