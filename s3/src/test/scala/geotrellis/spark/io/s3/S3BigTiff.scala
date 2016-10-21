package geotrellis.spark.io.s3

import geotrellis.util._
import geotrellis.spark.io.s3.util._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._


class S3BigTiffSpec extends FunSpec {
  describe("Reading bigtiff from s3") {
    val bucket = "bigtiffs-test"
    //val key = "aspect_byte_uncompressed_tiled_bigtiff.tif"
    //val key = "aspect_bit_uncompressed_striped_bigtiff.tif"
    val key = "3bands-striped-band-bigtiff.tif"
    val client = S3Client.default
    val chunkSize = 150
    /*
    val bucket = "gt-rasters"
    val key = "nlcd/2011/whole/nlcd_2011_landcover_2011_edition_2014_10_10.tif"
    val client = S3Client.default
    val chunkSize = 6500000
    */
    val s3Bytes = S3BytesStreamer(bucket, key, client, chunkSize)
    val reader = StreamByteReader(s3Bytes)
    val tiffTags = TiffTagsReader.read(reader)
    println(tiffTags.extent)
  }
}
