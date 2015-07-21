package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.op.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.testkit._

import org.scalatest._

class ByteGeoTiffMultiBandTileSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine
    with GeoTiffTestUtils 
    with TileBuilders {
  def p(s: String, i: String): String = 
    geoTiffPath(s"3bands/byte/3bands-${s}-${i}.tif")

  describe("ByteGeoTiffMultiBandTile") {

    // Combine all bands, int

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combine(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combine(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combine(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combine(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    // Combine all bands, double

    it("should combineDouble all bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    // Combine 2 bands, int

    it("should combine 2 bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combine(2, 1) { (z1, z2) => z1 + z2 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    // Combine 3 bands, double

    it("should combine 3 bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combineDouble(2, 1, 0) { (z1, z2, z3) => z1 + z2 - z3 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(4), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 + z2 - z3 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(0), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 * z2 - z3 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(-1), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 + (z2 * z3) }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(7), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    // Combine 4 bands, int

    it("should combine 4 bands with pixel interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "pixel")).tile

      val actual = tile.combineDouble(2, 1, 0, 2) { (z1, z2, z3, z4) => z1 + z2 - z3 + z4}
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(7), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with pixel interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(0, 1, 2, 0) { (z1, z2, z3, z4) => z1 + z2 - z3 + z4 }
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(1), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with band interleave, striped") {
      val tile =
        MultiBandGeoTiff.compressed(p("striped", "band")).tile

      val actual = tile.combineDouble(0, 1, 2, 0) { (z1, z2, z3, z4) => z1 * z2 - z3 - z4}
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(-2), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with band interleave, tiled") {
      val tile =
        MultiBandGeoTiff.compressed(p("tiled", "band")).tile

      val actual = tile.combineDouble(0, 1, 2, 2) { (z1, z2, z3, z4) => z1 + (z2 * z3) + z4}
      val expected = ByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(10), tile.cols, tile.rows)

      assertEqual(actual, expected)
    }

  }
}
