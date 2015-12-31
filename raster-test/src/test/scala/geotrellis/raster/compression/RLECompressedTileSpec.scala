package geotrellis.raster.compression

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class RLECompressedTileSpec extends FunSpec
    with TileBuilders
    with RasterMatchers {

  describe("RLE Compressed Tiles") {

    it("should compress and decompress a custom tile with RLE correctly #1") {
      val tile = ArrayTile(
        Array(1, 2, 3, 4, 5, 6, 7, 8, 9),
        3,
        3
      )

      val compressedTile = tile.compress(RLE)

      println(s"Compression ratio for RLE: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress a custom tile with RLE correctly #2") {
      val tile = ArrayTile(
        Array(1, 1, 1, 1, 1, 1, 1, 1, 1),
        3,
        3
      )

      val compressedTile = tile.compress(RLE)

      println(s"Compression ratio for RLE: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress aspect.tif with RLE correctly") {
      val tile = SingleBandGeoTiff("raster-test/data/aspect.tif").tile

      val compressedTile = tile.compress(RLE)

      println(s"Compression ratio for RLE on aspect.tif: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress slope.tif with RLE correctly") {
      val tile = SingleBandGeoTiff("raster-test/data/slope.tif").tile

      val compressedTile = tile.compress(RLE)

      println(s"Compression ratio for RLE on slope.tif: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }
  }
}
