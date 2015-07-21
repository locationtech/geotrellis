package geotrellis.raster.io.geotiff

import geotrellis.raster.TypeInt
import geotrellis.testkit.{TileBuilders, TestEngine}
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}


class UInt16GeoTiffTileSpec extends FunSpec
with Matchers
with BeforeAndAfterAll
with TestEngine
with GeoTiffTestUtils
with TileBuilders {

  describe("UInt16GeoTiffTile") {
   it("should read landsat 8 data correctly") {
     val actualImage = SingleBandGeoTiff(geoTiffPath(s"ls8_uint16.tif")).convert(TypeInt)
     val expectedImage = SingleBandGeoTiff(geoTiffPath(s"ls8_int32.tif"))

     assertEqual(actualImage.tile, expectedImage.tile)

   }
  }
}
