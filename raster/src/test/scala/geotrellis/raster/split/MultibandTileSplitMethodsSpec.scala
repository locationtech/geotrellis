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

import org.scalatest._

class MultibandTileSplitMethodsSpec extends FunSpec with Matchers with TileBuilders {
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

      val tl = TileLayout(3, 2, 3, 2)

      val result = tile.split(tl)

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          result(i).bandCount should be (3)
          val band = result(i).band(b)
          (band.cols, band.rows) should be ((3,2))
          band.foreach { z =>
            z should be ((i + 1) * math.pow(10, b))
          }
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

      val tl = TileLayout(3, 2, 3, 2)

      val result = tile.split(tl)

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          result(i).bandCount should be (3)
          val band = result(i).band(b)
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

      val tl = TileLayout(3, 2, 3, 2)

      val result = tile.split(tl, Split.Options(extend=false))

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          result(i).bandCount should be (3)
          val band = result(i).band(b)
          if(i == 2 || i == 5) {
            (band.cols, band.rows) should be ((2,2))
          } else {
            (band.cols, band.rows) should be ((3,2))
          }

          band.foreach { z =>
            z should be ((i + 1) * math.pow(10, b))
          }
        }
      }
    }

    it("should split a raster without by cols/rows") {
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

      val result = tile.split(3, 2)

      result.length should be (6)
      for(i <- 0 until 6) {
        for(b <- 0 until 3) {
          result(i).bandCount should be (3)
          val band = result(i).band(b)
          (band.cols, band.rows) should be ((3,2))
          band.foreach { z =>
            z should be ((i + 1) * math.pow(10, b))
          }
        }
      }
    }
  }
}
