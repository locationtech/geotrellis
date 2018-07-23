/*
 * Copyright 2018 Azavea
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

import geotrellis.util._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.compression.NoCompression
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest._

class GeoTiffBuilderSpec extends FunSpec with RasterMatchers with GeoTiffTestUtils {
  describe("GeoTiffBuilder") {
    val tileSize = 5
    val ct = IntCellType
    var expectedTile: MultibandTile = ArrayMultibandTile.empty(ct, 3, 15, 15)

    val segments =
      for {
        row <- 0 until 3
        col <- 0 until 3
      } yield {
        val tile = ArrayMultibandTile(
          ArrayTile(Array.ofDim[Int](tileSize*tileSize).fill(100 + (col*10)+(row)), tileSize, tileSize),
          ArrayTile(Array.ofDim[Int](tileSize*tileSize).fill(200 + (col*10)+(row)), tileSize, tileSize),
          ArrayTile(Array.ofDim[Int](tileSize*tileSize).fill(300 + (col*10)+(row)), tileSize, tileSize)
        )

        expectedTile = expectedTile.merge(tile, col * tileSize, row * tileSize)

        ((col, row), tile)
      }

    it("should build multiband tiffs with band interleave") {
      val segmentLayout = GeoTiffSegmentLayout(
        totalCols = 15,
        totalRows = 15,
        Tiled(tileSize, tileSize),
        BandInterleave,
        BandType.forCellType(ct))

      val tiff = GeoTiffBuilder[MultibandTile].makeTile(segments.toIterator, segmentLayout, ct, NoCompression)
      val actualTile = tiff.tile.toArrayTile

      assertEqual(expectedTile, actualTile)
    }

    it("should build multiband tiffs with pixel interleave") {
      val segmentLayout = GeoTiffSegmentLayout(
        totalCols = 15,
        totalRows = 15,
        Tiled(tileSize, tileSize),
        PixelInterleave,
        BandType.forCellType(ct))

      val tiff = GeoTiffBuilder[MultibandTile].makeTile(segments.toIterator, segmentLayout, ct, NoCompression)
      val actualTile = tiff.tile.toArrayTile

      assertEqual(expectedTile, actualTile)
    }
  }
}
