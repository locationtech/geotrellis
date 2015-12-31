package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.op.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.testkit._

import org.scalatest._

class BitGeoTiffMultiBandTileSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils 
    with TileBuilders {
  def p(s: String, i: String): String = 
    geoTiffPath(s"3bands/bit/3bands-${s}-${i}.tif")

  describe("BitGeoTiffMultiBandTile") {

    // Combine all bands, int

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

  }

  describe("BitGeoTiffMultiBandTile, decompressed") {

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff(p("striped", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combine(_.sum % 2)
      val expected = BitConstantTile(0, tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

  }
}
