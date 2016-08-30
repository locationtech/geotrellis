package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import com.amazonaws.services.s3.model._

import org.scalatest._

class S3ByteBufferSpec extends FunSpec with Matchers {
  val client = S3Client.default
  val bucket = "gt-rasters"
  val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
  val bb = S3ByteBuffer(bucket, k, client)

  describe("Getting data from S3ByteBuffer") {
    it("should get a byte") {
      val byte = bb.getArray(1)
      byte(0)
    }
    it("should read out multiple Array[Byte]s") {
      bb.getArray(15)
      bb.getArray(15)
      bb.getArray(15)
      bb.getArray(15)
    }
  }
}
