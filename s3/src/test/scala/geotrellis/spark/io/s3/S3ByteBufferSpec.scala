package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import geotrellis.util._
import com.amazonaws.services.s3.model._
import spire.syntax.cfor._

import org.scalatest._

class S3BytesByteReaderSpec extends FunSpec with Matchers {
  val client = S3Client.default
  val bucket = "gt-rasters"
  val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
  val byteBuffer = Filesystem.toMappedByteBuffer("../../nlcd_2011_01_01.tif")

  describe("MappedS3BytesByteReaderByteReader") {

    it("read in the first 4 bytes through get") {
      val s3BytesByteReader = S3BytesByteReader(bucket, k, client)
      cfor(0)(_ < 4, _ + 1){ i =>
        s3BytesByteReader.get
      }
    }
    
    /*

    it("should continue to read to the next MappedArray") {
      val s3BytesByteReader = S3BytesByteReader(bucket, k, client)
      cfor(0)(_ < 256002, _ + 1) { i =>
        s3BytesByteReader.get
      }
    }

    it("should be able to read at various parts of the file") {
      val s3BytesByteReader = S3BytesByteReader(bucket, k, client)
      val a = s3BytesByteReader.arr
      cfor(0)(_ < 25, _ + 1) { i =>
        s3BytesByteReader.get
      }
      val arr = Array.ofDim[Byte](980)
      s3BytesByteReader.position(400000)
      byteBuffer.position(4000000)
      val a2 = s3BytesByteReader.arr

      (a take 25).foreach(println)
      (a2 take 25).foreach(println)
      val expected = s3BytesByteReader.get
      val actual = byteBuffer.get

      byteBuffer.position(0)

      assert(actual == expected)
    }
  */
  }
}
