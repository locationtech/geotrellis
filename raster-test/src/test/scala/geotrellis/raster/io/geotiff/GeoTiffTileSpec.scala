package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.testkit._

import spire.syntax.cfor._
import org.scalatest._

class GeoTiffTileSpec extends FunSpec 
    with TestEngine
    with TileBuilders
    with GeoTiffTestUtils {

  describe("Creating a GeoTiff tile from an ArrayTile") {
    it("should work against a small int tile") {
      val t = createTile(Array(1, 2, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4, 4,
                               1, 2, 2, 1, 2, 1, 1, 2, 3, 2, 2, 3, 4, 3, 3, 4,
                               1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 2, 3, 4, 4, 3, 4,

                               4, 1, 4, 4, 3, 1, 3, 3, 2, 1, 2, 2, 1, 2, 1, 1,
                               4, 1, 1, 4, 3, 1, 1, 3, 2, 1, 1, 2, 1, 2, 2, 1,
                               4, 4, 1, 4, 3, 3, 1, 3, 2, 2, 1, 2, 1, 1, 2, 1,

                               2, 1, 2, 2, 3, 1, 3, 3, 4, 2, 4, 4, 1, 2, 1, 1,
                               2, 1, 1, 2, 3, 1, 1, 3, 4, 2, 2, 4, 1, 2, 2, 1,
                               2, 2, 1, 2, 3, 3, 1, 3, 4, 4, 2, 4, 1, 1, 2, 1), 16, 9)

      val gt = t.toGeoTiffTile()

      assertEqual(gt, t)
    }

    it("should work against econic.tif Striped NoCompression") {
      val options = GeoTiffOptions(Striped, NoCompression)
      val expected = SingleBandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }

    it("should work against econic.tif Striped with Deflate compression") {
      val options = GeoTiffOptions(Striped, DeflateCompression)

      val expected = SingleBandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }

    it("should work against econic.tif Tiled with no compression") {
      val options = GeoTiffOptions(Tiled, NoCompression)

      val expected = SingleBandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }

    it("should work against econic.tif Tiled with Deflate compression") {
      val options = GeoTiffOptions(Tiled, Deflate)

      val expected = SingleBandGeoTiff(s"$baseDataPath/econic.tif").tile
      val actual = expected.toGeoTiffTile(options)

      assertEqual(actual, expected)
    }
  }
}
