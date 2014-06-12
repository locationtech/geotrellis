/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis

import geotrellis.raster._
import geotrellis.source._
import geotrellis.process._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class TileRasterSpec extends FunSpec 
                        with RasterBuilders
                        with TestServer {
  describe("wrap") {
    it("wraps a literal raster") {
      val r = 
        createRaster(
          Array( 1,1,1, 2,2,2, 3,3,3,
                 1,1,1, 2,2,2, 3,3,3,
               
                 4,4,4, 5,5,5, 6,6,6,
                 4,4,4, 5,5,5, 6,6,6 ),
          9, 4)

      val tl = TileLayout(3,2,3,2)
      val tiled = TileRaster.wrap(r,tl)
      val tiles = tiled.tiles

      tiles.length should be (6)
      
      val values = collection.mutable.Set[Int]()
      for(tile <- tiles) {
        tile.rasterExtent.cols should be (3)
        tile.rasterExtent.rows should be (2)
        val arr = tile.toArray
        arr.toSet.size should be (1)
        values += arr(0)
      }
      values.toSeq.sorted.toSeq should be (Seq(1,2,3,4,5,6))
      
      assertEqual(r,tiled)
    }

    it("splits up a loaded raster") {
      val rOp = getRaster("elevation")
      val tOp = 
        rOp.map { r =>
          val (tcols,trows) = (11,20)
          val pcols = r.rasterExtent.cols / tcols
          val prows = r.rasterExtent.rows / trows
          val tl = TileLayout(tcols,trows,pcols,prows)
          TileRaster.wrap(r,tl)
        }
      val tiled = get(tOp)
      val raster = get(rOp)

      assertEqual(tiled,raster)
    }

    it("should wrap raster with cropped raster as tiles if cropped is true") {
      val name = "SBN_inc_percap"

      val r = RasterSource(name).get

      val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
      val tiled = TileRaster.wrap(r,tileLayout,cropped = true)
      tiled.tiles.map( t => t.asInstanceOf[CroppedRaster])
    }

    it("should wrap raster with array raster as tiles if cropped is false") {
      val name = "SBN_inc_percap"

      val r = RasterSource(name).get

      val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
      val tiled = TileRaster.wrap(r,tileLayout,cropped = false)
      tiled.tiles.map( t => t.asInstanceOf[ArrayRaster])
    }

    it("should wrap raster, and converge to same result warped to the tile layout") {
      val name = "SBN_inc_percap"

      val r = RasterSource(name).get

      val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
      val tiled = TileRaster.wrap(r,tileLayout,cropped = false)
      val backToArray = tiled.toArrayRaster

      for(col <- 0 until backToArray.cols optimized) {
        for(row <- 0 until backToArray.rows optimized) {
          if(col >= r.cols || row >= r.rows) {
            withClue (s"Tile grid coord $col,$row is out of raste bounds, so it should be NoData") {
              isNoData(backToArray.get(col,row)) should be (true)
            }
          } else {
            withClue (s"Value different at $col,$row: ") {
              backToArray.get(col,row) should be (r.get(col,row))
            }
          }
        }
      }
    }
  }
}
