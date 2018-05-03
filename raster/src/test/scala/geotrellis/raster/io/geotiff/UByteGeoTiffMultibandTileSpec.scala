/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.mapalgebra.local._

import geotrellis.vector.Extent

import geotrellis.proj4._

import geotrellis.raster.testkit._

import org.scalatest._

import java.util.Arrays

class ByteGeoTiffMultibandTileSpec extends FunSpec
    with Matchers with RasterMatchers
    with BeforeAndAfterAll
    with GeoTiffTestUtils 
    with TileBuilders {
  def p(s: String, i: String): String = 
    geoTiffPath(s"3bands/byte/3bands-${s}-${i}.tif")

  describe("UByteGeoTiffMultibandTile") {

    // Combine all bands, int

    it("should combine all bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combine(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine all bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combine(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combine(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine all bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combine(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    // Combine all bands, double

    it("should combineDouble all bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combineDouble all bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combineDouble(_.sum)
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(6), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    // Combine 2 bands, int

    it("should combine 2 bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combine(2, 1) { (z1, z2) => z1 + z2 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 2 bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combine(1, 2) { (z1, z2) => z1 + z2 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(5), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    // Combine 3 bands, double

    it("should combine 3 bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combineDouble(2, 1, 0) { (z1, z2, z3) => z1 + z2 - z3 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(4), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 + z2 - z3 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(0), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 * z2 - z3 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(-1), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 3 bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combineDouble(0, 1, 2) { (z1, z2, z3) => z1 + (z2 * z3) }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(7), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    // Combine 4 bands, int

    it("should combine 4 bands with pixel interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "pixel")).tile

      val actual = tile.combineDouble(2, 1, 0, 2) { (z1, z2, z3, z4) => z1 + z2 - z3 + z4}
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(7), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      val actual = tile.combineDouble(0, 1, 2, 0) { (z1, z2, z3, z4) => z1 + z2 - z3 + z4 }
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(1), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      val actual = tile.combineDouble(0, 1, 2, 0) { (z1, z2, z3, z4) => z1 * z2 - z3 - z4}
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(-2), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

    it("should combine 4 bands with band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      val actual = tile.combineDouble(0, 1, 2, 2) { (z1, z2, z3, z4) => z1 + (z2 * z3) + z4}
      val expected = UByteArrayTile(Array.ofDim[Byte](tile.cols * tile.rows).fill(10), tile.cols, tile.rows, UByteCellType)

      assertEqual(actual, expected)
    }

  }
}
