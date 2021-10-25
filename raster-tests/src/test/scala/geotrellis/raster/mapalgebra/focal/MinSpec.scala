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

class MinSpec extends AnyFunSpec with Matchers with FocalOpSpec with RasterMatchers {

  val getMinResult = Function.uncurried((getCursorResult _).curried((r,n) => Min.calculation(r,n)))

  describe("Tile focalMin") {
    val tile: Tile = createTile((0 until 16).toArray)

    it("should match scala.math.max default sets") {
      for(s <- defaultTestSets) {
        getMinResult(Square(1),MockCursor.fromAll(s:_*)) should equal ({
          val x = s.filter(isData(_))
          if(x.isEmpty) NODATA else x.min
        })
      }
    }

    it("square min r=1") {
      val expected = createTile(Array(
        0, 0, 1, 2,
        0, 0, 1, 2,
        4, 4, 5, 6,
        8, 8, 9, 10
      ))

      val actual = tile.focalMin(Square(1))

      assertEqual(actual, expected)
    }

    it("square min r=2") {
      val expected = Array(
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 0, 0, 1,
        4, 4, 4, 5)

      assertEqual(tile.focalMin(Square(2)), createTile(expected))
    }

    it("circle min r=2") {
      val expected = Array(
        0, 0, 0, 1,
        0, 0, 1, 2,
        0, 1, 2, 3,
        4, 5, 6, 7)

      assertEqual(tile.focalMin(Circle(2)), createTile(expected))
    }

    it("circle min r=3") {
      val r = createTile((0 until 16).toArray)
      assertEqual(r.focalMin(Circle(3)), Array(
        0, 0, 0, 0,
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 1, 2, 3))
    }

    it("circle min r=4+") {
      val r = createTile((0 until 16).toArray)
      val data0 = (0 until 16).map(z => 0).toArray
      assertEqual(r.focalMin(Circle(4)), Array(
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 1))
      assertEqual(r.focalMin(Circle(5)), data0)
      assertEqual(r.focalMin(Circle(6)), data0)
    }


    it("square min r=3+") {
      val r = createTile((0 until 16).toArray)
      val data0 = (0 until 16).map(z => 0).toArray
      assertEqual(r.focalMin(Square(3)), data0)
      assertEqual(r.focalMin(Square(4)), data0)
      assertEqual(r.focalMin(Square(5)), data0)
    }


    it("square min r=1 for CompositeTile") {
      val tile = Array(
        nd,7,1,     1,3,5,      9,8,2,
        9,1,1,      2,2,2,      4,3,5,

        3,8,1,      3,3,3,      1,2,2,
        2,4,7,     1,nd,1,      8,4,3)


      val expected = Array(
        1, 1, 1,    1, 1, 2,    2, 2, 2,
        1, 1, 1,    1, 1, 1,    1, 1, 2,

        1, 1, 1,    1, 1, 1,    1, 1, 2,
        2, 1, 1,    1, 1, 1,    1, 1, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Square(1)), createTile(expected, 9, 4))
    }

    it("square min for composite tile with double"){
      val tile = Array(
        NaN,7.1,1.2,      1.4,3.9,5.1,      9.9,8.1,2.2,
        9.4,1.1,1.5,      2.5,2.2,2.9,      4.0,3.3,5.1,

        3.4,8.2,1.9,      3.8,3.1,3.0,      1.3,2.1,2.5,
        2.5,4.9,7.1,      1.4,NaN,1.1,      8.0,4.8,3.0
      )

      val expected = Array(
        1.1, 1.1, 1.1,    1.2, 1.4, 2.2,    2.9, 2.2, 2.2,
        1.1, 1.1, 1.1,    1.2, 1.4, 1.3,    1.3, 1.3, 2.1,

        1.1, 1.1, 1.1,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1,
        2.5, 1.9, 1.4,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Square(1)), createTile(expected, 9, 4))
    }

    it("cicle min for composite tile with double"){
      val tile =Array(
        nd,7,4,     5, 4,2,      9,nd,nd,
        9,6,2,     2, 2,2,      5,3,nd,

        3,8,4,     3, 3,3,      3,9,2,
        2,9,7,     4,nd,9,      8,8,4
      )

      val expected = Array(
        7, 4, 2,    2, 2, 2,    2, 3, nd,
        3, 2, 2,    2, 2, 2,    2, 3, 2,

        2, 3, 2,    2, 2, 2,    3, 2, 2,
        2, 2, 4,    3, 3, 3,    3, 4, 2)

      val input = createCompositeTile(tile, TileLayout(3, 2, 3, 2))

      assertEqual(input.focalMin(Circle(1)), createTile(expected, 9, 4))
    }

    it("circle min with grid bounds"){
      val tile = createTile(Array(
        nd,     9,     6,     2,     2,
        nd,     3,     8,     4,     3,
        nd,     2,     9,     7,     4,
        nd,    nd,    nd,    nd,    nd
      ), 5, 4)

      val expected = createTile(Array(
        2, 3, 2,
        2, 2, 4
      ), 3, 2)


      assertEqual(tile.focalMin(Circle(1), Some(GridBounds(1,1,3,2))), expected)
    }

    it("should preserve source tile cell type for floating point tiles"){
      val cellType = FloatUserDefinedNoDataCellType(13.3f)
      val tile = ArrayTile.empty(cellType, 5, 5)
      val res = tile.focalMin(Square(1))
      assert(res.cellType == cellType)
      res.foreachDouble( v => assert(isNoData(v)) )
    }

    it("should preserve source tile cell type for integer tiles"){
      val cellType = ShortUserDefinedNoDataCellType(13)
      val tile = ArrayTile.empty(cellType, 5, 5)
      val res = tile.focalMin(Square(1))
      assert(res.cellType == cellType)
      res.foreach( v => assert(isNoData(v)) )
    }


  }


}
