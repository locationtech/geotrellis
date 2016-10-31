package geotrellis.spark.io.hadoop

import java.io.IOException

import geotrellis.util.Filesystem
import geotrellis.spark._
import org.scalatest._
import org.apache.hadoop.fs.Path

class HdfsUtilsSpec extends FunSpec
    with Matchers
    with TestEnvironment
{
  describe("HdfsUtils") {

    def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
      val zipped = arr1.zip(arr2)
      zipped.filter(x => x._1 != x._2)
    }

    val path = "spark/src/test/resources/all-ones.tif"
    val hdfsFile = new Path(path)
    val array = Filesystem.slurp(path)

    it("should not crash with unuseful error messages when no files match listFiles") {
      val path = new Path("/this/does/not/exist") // Would be really weird if this did.
      an[IOException] should be thrownBy { HdfsUtils.listFiles(path, conf) }
    }
    
    it("should read the wole file if given whole file length") {
      val actual = HdfsUtils.readRange(hdfsFile, 0, array.length, conf)
      
      val result = testArrays(array, actual)

      result.length should be (0)
    }
    
    it("should return an Array[Byte] of the correct size") {
      val actual = HdfsUtils.readRange(hdfsFile, 500, 500, conf)
      
      actual.length should be (500)
    }

    it("should read the correct range of bytes from a file") {
      val expected = Array.ofDim[Byte](500)
      val actual = HdfsUtils.readRange(hdfsFile, 500, 500, conf)
      
      System.arraycopy(array, 500, expected, 0, expected.length)

      val result = testArrays(expected, actual)

      result.length should be (0)
    }
  }
}
