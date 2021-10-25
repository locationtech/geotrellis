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

package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest.funspec.AnyFunSpec

import spire.syntax.cfor._

class CompositeTileSpec extends AnyFunSpec with TileBuilders with RasterMatchers with RasterTestFiles {
  describe("CompositeTileSpec wrap") {
    it("wraps a literal raster") {
      val r =
        createTile(
          Array( 1,1,1, 2,2,2, 3,3,3,
                 1,1,1, 2,2,2, 3,3,3,

                 4,4,4, 5,5,5, 6,6,6,
                 4,4,4, 5,5,5, 6,6,6 ),
          9, 4)

      val tl = TileLayout(3, 2, 3, 2)
      val tiled = CompositeTile.wrap(r, tl)
      val tiles = tiled.tiles

      tiles.length should be (6)

      val values = collection.mutable.Set[Int]()
      for(tile <- tiles) {
        tile.cols should be (3)
        tile.rows should be (2)
        val arr = tile.toArray()
        arr.toSet.size should be (1)
        values += arr(0)
      }
      values.toSeq.sorted should be (Seq(1, 2, 3, 4, 5, 6))

      assertEqual(r, tiled)
    }

    it("splits up a loaded raster") {
      val raster = loadTestArg("data/elevation").tile
      val (tcols, trows) = (11, 20)
      val pcols = raster.cols / tcols
      val prows = raster.rows / trows
      val tl = TileLayout(tcols, trows, pcols, prows)
      val tiled = CompositeTile.wrap(raster, tl)
      assertEqual(tiled, raster)
    }

    val r = loadTestArg("sbn/SBN_inc_percap")
    val rasterExtent = r.rasterExtent

    it("should wrap raster with cropped raster as tiles if cropped is true") {
      val tileLayout =
        TileLayout(
          rasterExtent.cols / 256,
          rasterExtent.rows / 256,
          256,
          256
        )
      val tiled = CompositeTile.wrap(r.tile, tileLayout, cropped = true)
      tiled.tiles.map( t => t.asInstanceOf[CroppedTile])
    }

    it("should wrap raster with array raster as tiles if cropped is false") {
      val tileLayout =
        TileLayout(
          rasterExtent.cols / 256,
          rasterExtent.rows / 256,
          256,
          256
        )
      val tiled = CompositeTile.wrap(r.tile, tileLayout, cropped = false)
      tiled.tiles.map( t => t.asInstanceOf[ArrayTile])
    }

    it("should wrap raster, and converge to same result resampleed to the tile layout") {
      val tileLayout =
        TileLayout(
          rasterExtent.cols / 256,
          rasterExtent.rows / 256,
          256,
          256
        )
      val tiled = CompositeTile.wrap(r.tile, tileLayout, cropped = false)
      val backToArray = tiled.toArrayTile()

      cfor(0)(_ < backToArray.rows, _ + 1) { row =>
        cfor(0)(_ < backToArray.cols, _ + 1) { col =>
          if(col >= r.cols || row >= r.rows) {
            withClue (s"Tile grid coord $col, $row is out of raste bounds, so it should be NoData") {
              isNoData(backToArray.get(col, row)) should be (true)
            }
          } else {
            withClue (s"Value different at $col, $row: ") {
              backToArray.get(col, row) should be (r.tile.get(col, row))
            }
          }
        }
      }
    }

    it("should wrap and combine to the same raster") {
      val totalCols = 1000
      val totalRows = 1500
      val tileCols = 2
      val tileRows = 1
      val pixelCols = totalCols / tileCols
      val pixelRows = totalRows / tileRows

      if( (pixelCols*tileCols, pixelRows*tileRows) != (totalCols, totalRows) )
        sys.error("This test requirest that the total col\rows be divisible by the tile col\rows")

      val (tile, extent) = {
        val (t, e) = (r.tile, rasterExtent.extent)
        (t.resample(e, totalCols, totalRows), e)
      }

      val tileLayout = TileLayout(tileCols, tileRows, pixelCols, pixelRows)

      val rasters: Seq[(Extent, Tile)] = {
        val tileExtents = TileExtents(extent, tileLayout)
        val tiles = CompositeTile.wrap(tile, tileLayout).tiles
        tiles.zipWithIndex.map { case (tile, i) => (tileExtents(i), tile) }
      }

      val actualExtent = rasters.map(_._1).reduce(_.combine(_))

      val actualTile =
        CompositeTile(rasters.map(_._2), tileLayout)


      actualExtent should be (extent)
      assertEqual(actualTile, tile)

    }
  }

  describe("CompositeTile cellType combine") {
    it("should union cellTypes") {
      val int = {
        val r =
          createTile(
            Array(1, 1, 1, 2, 2, 2, 3, 3, 3,
              1, 1, 1, 2, 2, 2, 3, 3, 3,

              4, 4, 4, 5, 5, 5, 6, 6, 6,
              4, 4, 4, 5, 5, 5, 6, 6, 6),
            9, 4)

        val tl = TileLayout(3, 2, 3, 2)
        CompositeTile.wrap(r, tl)
      }
      val dt = int.convert(DoubleCellType)

      int.combine(dt)(_ + _).cellType shouldBe int.cellType.union(dt.cellType)
    }
  }
}
