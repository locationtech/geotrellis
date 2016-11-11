package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff.reader._

import spire.syntax.cfor._
import org.scalatest._

class MultibandCropIteratorSpec extends FunSpec
  with Matchers
  with RasterMatchers
  with GeoTiffTestUtils {

  describe("Doing a crop iteration on a MultibandGeoTiff") {
    val path = geoTiffPath("3bands/3bands-striped-band.tif")
    val geoTiff = MultibandGeoTiff(path)
    val cols = geoTiff.imageData.cols
    val rows = geoTiff.imageData.rows

    it("should return the correct col and row iteration numbers for divisble subsections") {
      val windowedCols = 10
      val windowedRows = 20
      val multibandIterator = MultibandCropIterator(geoTiff, windowedCols, windowedRows)
      val actual = (multibandIterator.colIterations, multibandIterator.rowIterations)
      val expected = (cols / windowedCols, rows / windowedRows)

      actual should be (expected)
    }

    it("should return the correct col and row iteration numbers for nondivisble subsections") {
      val windowedCols = 700
      val windowedRows = 650
      val multibandIterator = MultibandCropIterator(geoTiff, windowedCols, windowedRows)
      val actual = (multibandIterator.colIterations, multibandIterator.rowIterations)
      val expected = (1, 1)

      actual should be (expected)
    }

    it("should return the correct windowedGeoTiffs with equal dimensions") {
      val windowedCols = 10
      val windowedRows = 10
      val multibandIterator =
        MultibandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected: Array[MultibandTile] =
        Array(geoTiff.raster.crop(0, 0, 10, 10),
          geoTiff.raster.crop(10, 0, 20, 10),
          geoTiff.raster.crop(0, 10, 10, 20),
          geoTiff.raster.crop(10, 10, 20, 20),
          geoTiff.raster.crop(0, 20, 10, 30),
          geoTiff.raster.crop(10, 20, 20, 30),
          geoTiff.raster.crop(0, 30, 10, 40),
          geoTiff.raster.crop(10, 30, 20, 40))

      val actual: Array[MultibandTile] =
        Array(multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile)

      cfor(0)(_ < actual.length, _ + 1) { i =>
        assertEqual(expected(i), actual(i))
      }
    }

    it("should return the whole thing if the inputted dimensions are larger than the cols and rows") {
      val windowedCols = 25
      val windowedRows = 50
      val multibandIterator = MultibandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected = geoTiff.tile
      val actual = multibandIterator.next.tile

      assertEqual(expected, actual)
    }

    it("should return the correct windowedGeoTiffs with different dimensions") {
      val windowedCols = 15
      val windowedRows = 25
      val multibandIterator =
        new MultibandCropIterator(geoTiff, windowedCols, windowedRows)

      val expected: Array[MultibandTile] =
        Array(geoTiff.raster.crop(0, 0, 15, 25),
          geoTiff.raster.crop(15, 0, 20, 25),
          geoTiff.raster.crop(0, 25, 15, 40),
          geoTiff.raster.crop(15, 25, 20, 40))

      val actual: Array[MultibandTile] =
        Array(multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile,
          multibandIterator.next.tile)

      cfor(0)(_ < actual.length, _ + 1) { i =>
        assertEqual(expected(i), actual(i))
      }
    }

    it("should say that there is another value when one actually exists") {
      val windowedCols = 15
      val windowedRows = 25
      val multibandIterator =
        new MultibandCropIterator(geoTiff, windowedCols, windowedRows)

      cfor(0)(_ < 3, _ + 1) { i =>
        multibandIterator.next.tile
        multibandIterator.hasNext should be (true)
      }
      multibandIterator.next.tile
      multibandIterator.hasNext should be (false)
    }
  }
}
