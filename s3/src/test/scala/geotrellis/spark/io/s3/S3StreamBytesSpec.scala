package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._

import scala.collection.mutable._
import java.nio.ByteBuffer

import spire.syntax.cfor._
import com.amazonaws.services.s3.model._

import org.scalatest._

class S3StreamBytesSpec extends FunSpec with Matchers with S3StreamBytes {
  val client = S3Client.default
  val bucket = "gt-rasters"
  val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
  val request = new GetObjectRequest(bucket, k)

  def arraysMatch[A <: Any](a1: Array[A], a2: Array[A]): Boolean = {
    val zipped = a1 zip a2
    val result = zipped.filter(x => x._1 != x._2)

    if (result.length == 0) true else false
  }

  describe("Readig the Stream from S3") {

    it("should be able to create a default chunk from the begininng") {
      val actual = getArray
      val expected = getArray(0, 256000)

      assert(arraysMatch(expected, actual))
    }

    it("should read ahead and then back") {
      val actual = getArray(150, 250)
      getArray(500, 750)
      val expected = getArray(150, 250)

      assert(arraysMatch(expected, actual))
    }

    it("should return the correct offset for each iteration") {
      val actual = Array[Long](0, 256000, 512000, 768000, 1024000)
      val listBuffer = ListBuffer[Long]()

      cfor(0)(_ < 1024001, _ + 256000) { i =>
        val mapped = getMappedArray(i)
        listBuffer += mapped.head._1
      }

      assert(arraysMatch(listBuffer.toArray, actual))
    }
  }
}
