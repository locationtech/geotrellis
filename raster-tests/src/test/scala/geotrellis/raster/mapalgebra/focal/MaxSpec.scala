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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

import geotrellis.raster.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class MaxSpec extends AnyFunSpec with Matchers with TileBuilders with RasterMatchers with FocalOpSpec {

  val getMaxResult = Function.uncurried((getCursorResult _).curried(
    (r,n) => Max.calculation(r,n)
  ))
  val getMaxSetup = Function.uncurried((getSetup _).curried(
    (r,n) => Max.calculation(r,n)
  ))
  val squareSetup = getMaxSetup(defaultRaster, Square(1))

  describe("Tile focalMax") {
    it("should correctly compute a center neighborhood") {
      squareSetup.result(2,2) should equal (4)
    }

    it("should match scala.math.max default sets") {
      for(s <- defaultTestSets) {
        getMaxResult(Square(1),MockCursor.fromAll(s:_*)) should equal (s.max)
      }
    }

    it("square max r=1") {
      val tile: Tile = createTile(Array[Int](
        1,1,1,1,
        2,2,2,2,
        3,3,3,3,
        1,1,4,4))

      val expected = Array[Int](
        2,2,2,2,
        3,3,3,3,
        3,4,4,4,
        3,4,4,4)

      tile.focalMax(Square(1)) should be (createTile(expected))
    }

    it("square min r=1 on doubles") {
      val input = createTile(Array(
        1.2,1.3,1.1,1.4,
        2.4,2.1,2.5,2.2,
        3.1,3.5,3.2,3.1,
        1.9,1.1,4.4,4.9))

      val expected = Array[Double](
        2.4,2.5,2.5,2.5,
        3.5,3.5,3.5,3.2,
        3.5,4.4,4.9,4.9,
        3.5,4.4,4.9,4.9)

      assertEqual(input.focalMax(Square(1)), createTile(expected))
    }

    it("square max r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)


      val expected = Array(
        9, 9, 7,    2, 2, 2,    3, 3, 3,
        9, 9, 8,    3, 3, 3,    3, 3, 3,

        9, 9, 8,    7, 3, 8,    8, 8, 3,
        8, 8, 8,    7, 3, 8,    8, 8, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Square(1)), createTile(expected, 9, 4))
    }

    it("square max r=2 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)


      val expected = Array(
        9, 9, 9,    8, 3, 3,    3, 3, 3,
        9, 9, 9,    8, 8, 8,    8, 8, 8,

        9, 9, 9,    8, 8, 8,    8, 8, 8,
        9, 9, 9,    8, 8, 8,    8, 8, 8)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Square(2)), createTile(expected, 9, 4))
    }

    it("circle max r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,1,1,      1,1,1,
        9,1,1,      2,2,2,      1,3,1,

        3,8,1,      3,3,3,      1,1,2,
        2,1,7,     1,nd,1,      8,1,1)

      val expected = Array(
        9, 7, 7,    2, 2, 2,    1, 3, 1,
        9, 9, 2,    3, 3, 3,    3, 3, 3,

        9, 8, 8,    3, 3, 3,    8, 3, 2,
        3, 8, 7,    7, 3, 8,    8, 8, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))
      assertEqual(input.focalMax(Circle(1)), createTile(expected, 9, 4))
    }

    it("should perserve NoData values for Double tiles"){
      val tile = ArrayTile.empty(DoubleConstantNoDataCellType, 5, 5)
      val res = tile.focalMax(Square(1))
      res.foreach( v => assert(isNoData(v)) )
    }

    it("should preserve source tile cell type for floating point tiles"){
      val cellType = FloatUserDefinedNoDataCellType(13.3f)
      val tile = ArrayTile.empty(cellType, 5, 5)
      val res = tile.focalMax(Square(1))
      assert(res.cellType == cellType)
      res.foreachDouble( v => assert(isNoData(v)) )
    }

    it("should preserve source tile cell type for integer tiles"){
      val cellType = ShortUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      val res = tile.focalMax(Square(1))
      assert(res.cellType == cellType)
      res.foreach( v => assert(isNoData(v)) )
    }
  }
}
