package geotrellis.spark.io.s3.util

import geotrellis.util.Filesystem
import geotrellis.spark.io.s3._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._
import com.amazonaws.services.s3.model._
import spire.syntax.cfor._

class S3TiffTagsReaderSpec extends FunSpec {

  describe("tifftags reader") {
    val client = S3Client.default
    val bucket = "gt-rasters"
    val k = "nlcd/2011/tiles/nlcd_2011_01_01.tif"
    val s3ByteBuffer = S3BytesByteReader(bucket, k, client)
    val fromLocal = TiffTagsReader.read("../../nlcd_2011_01_01.tif")
    val fromServer = TiffTagsReader.read(s3ByteBuffer)

    /*
    it("should read the same basic tags") {
      val expected = fromServer.basicTags
      val actual = fromLocal.basicTags

      assert(expected == actual)
    }
    
    it("should read the same colimetry tags") {
      val expected = fromServer.colimetryTags
      val actual = fromLocal.colimetryTags

      assert(expected == actual)
    }
    
    it("should read the same cmyk tags") {
      val expected = fromServer.cmykTags
      val actual = fromLocal.cmykTags

      assert(expected == actual)
    }
    
    it("should read the same dataSampleForamt tags") {
      val expected = fromServer.dataSampleFormatTags
      val actual = fromLocal.dataSampleFormatTags

      assert(expected == actual)
    }
    
    it("should read the same documentation tags") {
      val expected = fromServer.documentationTags
      val actual = fromLocal.documentationTags

      assert(expected == actual)
    }
    
    it("should read the same geoTiffTags tags") {
      val expected = fromServer.geoTiffTags
      val actual = fromLocal.geoTiffTags

      assert(expected == actual)
    }
    
    it("should read the same jpegTags tags") {
      val expected = fromServer.jpegTags
      val actual = fromLocal.jpegTags

      assert(expected == actual)
    }
    it("should read the same metadata tags") {
      val expected = fromServer.metadataTags
      val actual = fromLocal.metadataTags

      assert(expected == actual)
    }

    it("should read the same nonBasic tags") {
      val expected = fromServer.nonBasicTags
      val actual = fromLocal.nonBasicTags

      assert(expected == actual)
    }
    
    it("should read the same nonStandardized tags") {
      val expected = fromServer.nonStandardizedTags
      val actual = fromLocal.nonStandardizedTags

      assert(expected == actual)
    }

    it("should read the same tags tags") {
      val expected = fromServer.tags
      val actual = fromLocal.tags

      assert(expected == actual)
    }
    
    it("should read the same tile tags") {
      val expected = fromServer.tileTags
      val actual = fromLocal.tileTags

      assert(expected == actual)
    }
    
    it("should read the same ycbcr tags") {
      val expected = fromServer.yCbCrTags
      val actual = fromLocal.yCbCrTags

      assert(expected == actual)
    }
    */
  }
}
