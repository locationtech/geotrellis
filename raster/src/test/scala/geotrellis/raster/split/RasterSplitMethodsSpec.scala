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

package geotrellis.raster.split

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.vector._

import org.scalatest._

class RasterSplitMethodsSpec extends FunSpec with Matchers with TileBuilders {
  describe("MultibandTile split") {
    it("should split a raster") {
      val b1 =
        createTile(
          Array( 1,1,1, 2,2,2, 3,3,3,
                 1,1,1, 2,2,2, 3,3,3,

                 4,4,4, 5,5,5, 6,6,6,
                 4,4,4, 5,5,5, 6,6,6 ),
          9, 4)

      val b2 = b1 * 10
      val b3 = b2 * 10

      val tile = MultibandTile(b1, b2, b3)
      val e = Extent(0.0, 0.0, 9.0, 10.0)

      val tl = TileLayout(3, 2, 3, 2)

      val result = Raster(tile: MultibandTile, e).split(tl)

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          val Raster(resultTile, resultExtent) = result(i)
          resultTile.bandCount should be (3)
          val band = result(i).tile.band(b)
          (band.cols, band.rows) should be ((3,2))
          band.foreach { z =>
            z should be ((i + 1) * math.pow(10, b))
          }

          val xmin = 0.0 + (i % 3 * 3.0)
          val ymax = 10.0 - ((i / 3) * 5.0)
          val xmax = xmin + 3.0
          val ymin = ymax - 5.0

          resultExtent should be (Extent(xmin, ymin, xmax, ymax))
        }
      }
    }

    it("should split a raster with a tile layout that extends past the original raster bounds") {
      val b1 =
        createTile(
          Array( 1,1,1, 2,2,2, 3,3,
                 1,1,1, 2,2,2, 3,3,

                 4,4,4, 5,5,5, 6,6,
                 4,4,4, 5,5,5, 6,6 ),
          8, 4)

      val b2 = b1 * 10
      val b3 = b2 * 10

      val tile = MultibandTile(b1, b2, b3)
      val e = Extent(0.0, 0.0, 8.0, 10.0)

      val tl = TileLayout(3, 2, 3, 2)

      val result = Raster(tile: MultibandTile, e).split(tl)

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          val Raster(resultTile, resultExtent) = result(i)
          resultTile.bandCount should be (3)
          val band = resultTile.band(b)
          (band.cols, band.rows) should be ((3,2))

          if(i == 2 || i == 5) {
            band.foreach { (col, row, z) =>
              if(col == 2) { z should be (NODATA) }
              else { z should be ((i + 1) * math.pow(10, b)) }
            }
          } else {
            band.foreach { z =>
              z should be ((i + 1) * math.pow(10, b))
            }
          }

          val xmin = 0.0 + (i % 3 * 3.0)
          val ymax = 10.0 - ((i / 3) * 5.0)
          val xmax = xmin + 3.0
          val ymin = ymax - 5.0

          resultExtent should be (Extent(xmin, ymin, xmax, ymax))
        }
      }
    }

    it("should split a raster with a tile layout that doesn't extend when extends=false") {
      val b1 =
        createTile(
          Array( 1,1,1, 2,2,2, 3,3,
                 1,1,1, 2,2,2, 3,3,

                 4,4,4, 5,5,5, 6,6,
                 4,4,4, 5,5,5, 6,6 ),
          8, 4)

      val b2 = b1 * 10
      val b3 = b2 * 10

      val tile = MultibandTile(b1, b2, b3)
      val e = Extent(0.0, 0.0, 8.0, 10.0)

      val tl = TileLayout(3, 2, 3, 2)

      val result = Raster(tile: MultibandTile, e).split(tl, Split.Options(extend=false))

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          val Raster(resultTile, resultExtent) = result(i)
          resultTile.bandCount should be (3)
          val band = resultTile.band(b)
          if(i == 2 || i == 5) {
            val xmin = 6.0
            val ymax = 10.0 - ((i / 3) * 5.0)
            val xmax = 8.0
            val ymin = ymax - 5.0

            resultExtent should be (Extent(xmin, ymin, xmax, ymax))
            (band.cols, band.rows) should be ((2,2))
          } else {
            val xmin = 0.0 + (i % 3 * 3.0)
            val ymax = 10.0 - ((i / 3) * 5.0)
            val xmax = xmin + 3.0
            val ymin = ymax - 5.0

            resultExtent should be (Extent(xmin, ymin, xmax, ymax))
            (band.cols, band.rows) should be ((3,2))
          }

          band.foreach { z =>
            z should be ((i + 1) * math.pow(10, b))
          }
        }
      }
  }
  }
}
