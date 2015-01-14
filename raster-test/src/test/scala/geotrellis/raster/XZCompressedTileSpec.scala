package geotrellis.raster

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._

import geotrellis.engine._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class XZCompressedTileSpec extends FunSpec
    with TileBuilders
    with TestEngine
    with RasterMatchers {

  describe("XZ Compressed Tiles") {

    it("should compress and decompress a custom tile with XZ correctly #1") {
      val tile = ArrayTile(
        Array(1, 2, 3, 4, 5, 6, 7, 8, 9),
        3,
        3
      )

      val compressedTile = tile.compress(XZ)

      println(s"Compression ratio for XZ: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress a custom tile with XZ correctly #2") {
      val tile = ArrayTile(
        Array(1, 1, 1, 1, 1, 1, 1, 1, 1),
        3,
        3
      )

      val compressedTile = tile.compress(XZ)

      println(s"Compression ratio for XZ: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress aspect.tif with XZ correctly") {
      val (tile, _, _) = GeoTiffReader("raster-test/data/aspect.tif")
        .read.imageDirectories.head.toRaster

      val compressedTile = tile.compress(XZ)

      println(s"Compression ratio for XZ on aspect.tif: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }

    it("should compress and decompress slope.tif with XZ correctly") {
      val (tile, _, _) = GeoTiffReader("raster-test/data/slope.tif")
        .read.imageDirectories.head.toRaster

      val compressedTile = tile.compress(XZ)

      println(s"Compression ratio for XZ on slope.tif: ${compressedTile.compressionRatio}")

      val decompressedTile = compressedTile.decompress

      tilesEqual(tile, decompressedTile)
    }
  }
}
