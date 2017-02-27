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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.viewshed.R2Viewshed._

import org.scalatest._


class R2ViewshedSpec extends FunSpec
    with Matchers
    with RasterMatchers with TestFiles
    with TileBuilders {

  describe("Viewshed") {

    val elevationTile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val viewshedTile = ArrayTile.empty(IntCellType, 5, 5)

    it("propogates up") {
      val left: Array[Ray] = Array(Ray(1000000, 7), Ray(Double.PositiveInfinity, 13))
      val right: Array[Ray] = Array(Ray(1000000, 22))
      var a: Int = 0
      var b: Int = 0
      var c: Int = 0
      var all: Int = 0

      R2Viewshed.compute(
        elevationTile, viewshedTile,
        2, 7, 0, 1,
        FromSouth, left, right,
        { case Ray(m, alpha) =>
          all += 1
          if (alpha == 7) a += 1
          else if (alpha == 13) b += 1
          else if (alpha == 22) c += 1
          else throw new Exception
        })

      a should be (6)
      b should be (1)
      c should be (6)
      all should be (13)
    }

    it("propogates right") {
      val left: Array[Ray] = Array(Ray(1, 7))
      val right: Array[Ray] = Array(Ray(0, 13), Ray(1, 22))
      var a: Int = 0
      var b: Int = 0
      var c: Int = 0
      var all: Int = 0

      R2Viewshed.compute(
        elevationTile, viewshedTile,
        -3, 2, 0, 1,
        FromWest, left, right,
        { case Ray(m, alpha) =>
          all += 1
          if (alpha == 7) a += 1
          else if (alpha == 13) b += 1
          else if (alpha == 22) c += 1
          else throw new Exception
        })

      a should be (6)
      b should be (1)
      c should be (6)
      all should be (13)
    }

    it("propogates down") {
      val left: Array[Ray] = Array(Ray(1000000, 22))
      val right: Array[Ray] = Array(Ray(1000000, 7), Ray(Double.PositiveInfinity, 13))
      var a: Int = 0
      var b: Int = 0
      var c: Int = 0
      var all: Int = 0

      R2Viewshed.compute(
        elevationTile, viewshedTile,
        2, -3, 0, 1,
        FromNorth, left, right,
        { case Ray(m, alpha) =>
          all += 1
          if (alpha == 7) a += 1
          else if (alpha == 13) b += 1
          else if (alpha == 22) c += 1
          else throw new Exception
        })

      a should be (6)
      b should be (1)
      c should be (6)
      all should be (13)
    }

    it("propogates left") {
      val left: Array[Ray] = Array(Ray(1, 7))
      val right: Array[Ray] = Array(Ray(0, 13), Ray(1, 22))
      var a: Int = 0
      var b: Int = 0
      var c: Int = 0
      var all: Int = 0

      R2Viewshed.compute(
        elevationTile, viewshedTile,
        7, 2, 0, 1,
        FromEast, left, right,
        { case Ray(m, alpha) =>
          all += 1
          if (alpha == 7) a += 1
          else if (alpha == 13) b += 1
          else if (alpha == 22) c += 1
          else throw new Exception
        })

      a should be (6)
      b should be (1)
      c should be (6)
      all should be (13)
    }

    it("computes the viewshed of a flat int plane") {
      val r = createTile(Array.fill(7 * 8)(1), 7, 8)
      val shed = R2Viewshed(r, 4, 3)
      assertEqual(BitConstantTile(true, 7, 8), shed)
    }

    it("computes the viewshed of a flat double plane") {
      val r = createTile(Array.fill(7 * 8)(1.5), 7, 8)
      val shed = R2Viewshed(r, 4, 3)
      assertEqual(BitConstantTile(true, 7, 8), shed)
    }

    it("computes the viewshed of a double line") {
      val rasterData = Array (
        300.0, 1.0, 99.0, 0.0, 10.0, 200.0, 137.0
      )
      val viewable = Array (
        1, 0, 1, 1, 1, 1, 0
      )
      val r = createTile(rasterData, 7, 1)
      val viewRaster = createTile(viewable, 7, 1).convert(BitCellType)
      val shed = R2Viewshed(r, 3, 0)
      assertEqual(viewRaster, shed)
    }

    it("computes the viewshed of a double plane") {
      val rasterData = Array (
        999.0, 1.0,   1.0,   1.0,   1.0,   1.0,   999.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   499.0, 1.0,
        1.0,   1.0,   1.0,   1.0,   99.0,  1.0,   1.0,
        1.0,   1.0,   999.0, 1.0,   1.0,   1.0,   1.0,
        1.0,   1.0,   1.0,   1.0,   100.0, 1.0,   1.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   101.0, 1.0,
        1.0,   1.0,   1.0,   1.0,   1.0,   1.0,   102.0
      )
      val viewable = Array (
        1,     1,     1,     1,     1,     0,     1,
        1,     1,     1,     1,     1,     1,     0,
        0,     1,     1,     1,     1,     1,     1,
        0,     0,     1,     1,     1,     1,     1,
        0,     1,     1,     1,     1,     1,     1,
        1,     1,     1,     1,     1,     0,     0,
        1,     1,     1,     1,     1,     0,     0
      )
      val r = createTile(rasterData, 7, 7)
      val viewRaster = createTile(viewable, 7, 7).convert(BitCellType)
      val shed = R2Viewshed(r, 3, 3)
      assertEqual(viewRaster, shed)
    }

    it("computes the viewshed of a int plane") {
      val rasterData = Array (
        999, 1,   1,   1,   1,   499, 999,
        1,   1,   1,   1,   1,   499, 250,
        1,   1,   1,   1,   99,  1,   1,
        1,   999, 1,   1,   1,   1,   1,
        1,   1,   1,   1,   1,   1,   1,
        1,   1,   1,   0,   1,   1,   1,
        1,   1,   1,   1,   1,   1,   1
      )
      val viewable = Array (
        1,     1,     1,     1,     1,     1,     1,
        1,     1,     1,     1,     1,     1,     0,
        1,     1,     1,     1,     1,     1,     1,
        0,     1,     1,     1,     1,     1,     1,
        1,     1,     1,     1,     1,     1,     1,
        1,     1,     1,     0,     1,     1,     1,
        1,     1,     1,     1,     1,     1,     1
      )
      val r = createTile(rasterData, 7, 7)
      val viewRaster = createTile(viewable, 7, 7).convert(BitCellType)
      val shed = R2Viewshed(r, 3, 3)
      assertEqual(viewRaster, shed)
    }

    it("ignores NoData values and indicates they're unviewable"){
      val rasterData = Array (
        300.0, 1.0, 99.0, 0.0, Double.NaN, 200.0, 137.0
      )
      val viewable = Array (
        1, 0, 1, 1, 0, 1, 0
      )
      val r = createTile(rasterData, 7, 1)
      val viewRaster = createTile(viewable, 7, 1).convert(BitCellType)
      val shed = R2Viewshed(r, 3, 0)
      assertEqual(viewRaster, shed)
    }

    it("should match veiwshed generated by ArgGIS") {
      val rs = loadTestArg("data/viewshed-elevation")
      val elevation = rs.tile
      val rasterExtent = rs.rasterExtent
      val expected = loadTestArg("data/viewshed-expected")

      val (x, y) = (-93.63300872055451407, 30.54649743277299123) // create overload
      val (col, row) = rasterExtent.mapToGrid(x, y)
      val actual = R2Viewshed(elevation, col, row)

      def countDiff(a: Tile, b: Tile): Int = {
        var ans = 0
        for(col <- 0 until 256) {
          for(row <- 0 until 256) {
            if (a.get(col, row) != b.get(col, row)) ans += 1;
          }
        }
        ans
      }

      val diff = (countDiff(expected, actual) / (256 * 256).toDouble) * 100
      val allowable = 8.75
      // System.out.println(s"${diff} / ${256 * 256} = ${diff / (256 * 256).toDouble}")
      withClue(s"Percent difference from ArgGIS raster is more than $allowable%:") {
        diff should be < allowable
      }
    }
  }
}
