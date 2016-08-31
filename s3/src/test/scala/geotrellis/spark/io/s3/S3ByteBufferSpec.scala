package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._
import spire.syntax.cfor._

import org.scalatest._

class S3ByteBufferSpec extends FunSpec with Matchers {
  val client = S3Client.default
  val bucket = "gt-rasters"
  val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"

  describe("MappedS3ByteBuffer") {

    it("read in the first 4 bytes through get") {
      val bb = S3ByteBuffer(bucket, k, client)
      cfor(0)(_ < 4, _ + 1){ i =>
        bb.get
      }
    }

    it("should continue to read to the next MappedArray") {
      val bb = S3ByteBuffer(bucket, k, client)
      cfor(0)(_ < 256002, _ + 1) { i =>
        bb.get
      }
    }
    
    it("read in the first 4 bytes through bulk get") {
      val bb = S3ByteBuffer(bucket, k, client)
      val arr = Array.ofDim[Byte](4)
      bb.get(arr, 0, arr.length)
    }

    it("should return the correct position") {
      val bb = S3ByteBuffer(bucket, k, client)
      val arr = Array.ofDim[Byte](950)
      bb.get(arr, 789, arr.length)

      assert(bb.location == bb.position)
    }
  }
}
