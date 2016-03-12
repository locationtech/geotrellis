package geotrellis.raster.io.geotiff

import geotrellis.raster.testkit.{RasterMatchers, TileBuilders}
import geotrellis.raster.IntCellType
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

class UInt16GeoTiffTileSpec extends FunSpec
with Matchers
with BeforeAndAfterAll
with RasterMatchers
with GeoTiffTestUtils
with TileBuilders {
  describe("UInt16GeoTiffTile") {
   it("should read landsat 8 data correctly") {
     val actualImage = SinglebandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).convert(IntCellType)
     val expectedImage = SinglebandGeoTiff(geoTiffPath(s"ls8_int32.tif"))

     assertEqual(actualImage.tile, expectedImage.tile)
   }
  }
}
