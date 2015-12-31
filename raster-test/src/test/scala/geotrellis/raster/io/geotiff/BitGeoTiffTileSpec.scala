package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.op.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.testkit._

import org.scalatest._

class BitGeoTiffTileSpec extends FunSpec
    with Matchers
    with RasterMatchers
    with BeforeAndAfterAll
    with GeoTiffTestUtils 
    with TileBuilders {
  describe("BitGeoTiffTile") {
    it("should map same") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif")).tile

      val res = tile.map { z => z }

      assertEqual(res, tile)
    }

    it("should map inverse") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif")).tile

      val res = tile.map { z => z ^ 1 }.map { z => z ^ 1 }

      assertEqual(res, tile)
    }

    it("should map with index") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif")).tile

      val res = tile.map { (col, row, z) => z }

      assertEqual(res, tile)
    }

    it("should map with index double") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif")).tile

      val res = tile.mapDouble { (col, row, z) => z }

      assertEqual(res, tile)
    }

    it("should map with index - tiled") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel_tiled.tif")).tile

      val res = tile.map { (col, row, z) => z }

      assertEqual(res, tile)
    }

    it("should map with index double - tiled") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("bilevel_tiled.tif")).tile

      val res = tile.mapDouble { (col, row, z) => z }

      assertEqual(res, tile)
    }
  }
}
