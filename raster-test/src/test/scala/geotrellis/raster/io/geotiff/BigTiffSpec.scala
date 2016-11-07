package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest._

class BigTiffSpec extends FunSpec with RasterMatchers with GeoTiffTestUtils {
  describe("Reading BigTiffs") {
    val smallPath = "raster-test/data/geotiff-test-files/ls8_int32.tif"
    val bigPath = "raster-test/data/geotiff-test-files/bigtiffs/ls8_int32-big.tif"

    val smallPathMulti = "raster-test/data/geotiff-test-files/multi.tif"
    val bigPathMulti = "raster-test/data/geotiff-test-files/bigtiffs/multi-big.tif"

    val chunkSize = 500

    it("should read in the entire SinglebandGeoTiff") {
      val local = LocalBytesStreamer(bigPath, chunkSize)
      val reader = StreamByteReader(local)
      val actual = SinglebandGeoTiff(reader)
      val expected = SinglebandGeoTiff(smallPath)

      assertEqual(actual.tile, expected.tile)
    }

    it("should read in a cropped SinlebandGeoTiff from the edge") {
      val local = LocalBytesStreamer(bigPath, chunkSize)
      val reader = StreamByteReader(local)
      val tiffTags = TiffTagsReader.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile, expected.tile)
    }

    it("should read in a cropped SinglebandGeoTiff in the middle") {
      val local = LocalBytesStreamer(bigPath, chunkSize)
      val reader = StreamByteReader(local)
      val tiffTags = TiffTagsReader.read(smallPath)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = SinglebandGeoTiff(reader, e)
      val expected = SinglebandGeoTiff(smallPath, e)

      assertEqual(actual.tile, expected.tile)
    }

    it("should read in the entire MultibandGeoTiff") {
      val local = LocalBytesStreamer(bigPathMulti, chunkSize)
      val reader = StreamByteReader(local)
      val actual = MultibandGeoTiff(reader)
      val expected = MultibandGeoTiff(smallPathMulti)

      assertEqual(actual.tile, expected.tile)
    }

    it("should read in a cropped MultibandGeoTiff from the edge") {
      val local = LocalBytesStreamer(bigPathMulti, chunkSize)
      val reader = StreamByteReader(local)
      val tiffTags = TiffTagsReader.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin, extent.ymin, extent.xmin + 100, extent.ymin + 100)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile, expected.tile)
    }

    it("should read in a cropped MultibandGeoTiff in the middle") {
      val local = LocalBytesStreamer(bigPathMulti, chunkSize)
      val reader = StreamByteReader(local)
      val tiffTags = TiffTagsReader.read(smallPathMulti)
      val extent = tiffTags.extent
      val e = Extent(extent.xmin + 100 , extent.ymin + 100, extent.xmax - 250, extent.ymax - 250)

      val actual = MultibandGeoTiff(reader, e)
      val expected = MultibandGeoTiff(smallPathMulti, e)

      assertEqual(actual.tile, expected.tile)
    }
  }
}
