package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.op.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.testkit._

import org.scalatest._

class Int32GeoTiffMultiBandTileSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils 
    with TileBuilders {
  def p(s: String, i: String): String = 
    geoTiffPath(s"3bands/int32/3bands-${s}-${i}.tif")

  describe("Int32GeoTiffMultiBandTile") {

    // Combine all bands, double

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = IntArrayTile(Array.ofDim[Int](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = IntArrayTile(Array.ofDim[Int](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff(p("striped", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = IntArrayTile(Array.ofDim[Int](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = IntArrayTile(Array.ofDim[Int](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }
  }
}
