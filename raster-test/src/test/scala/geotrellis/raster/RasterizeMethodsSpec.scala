/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster

import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.testkit._
import geotrellis.vector.{Extent, Feature}

import org.scalatest._


class RasterizeMethodsSpec extends FunSpec
    with Matchers
    with TileBuilders {

  import geotrellis.vector.{Point,Line,Polygon}

  val magicNumber = 42
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val point = Point(1,9)
  val line = Line((3,7), (6,4), (3,1), (0,4))
  val square  = Polygon( Line((1,9), (1,6), (4,6), (4,9), (1,9)) )
  val diamond = Polygon( Line((3,7), (6,4), (3,1), (0,4), (3,7)))
  val triangle = Polygon( Line((2,8),(5,5),(6,7), (6,7), (2,8)))

  val pointExpected = Rasterizer.rasterizeWithValue(point, re, magicNumber).toArray.filter(_ == magicNumber).length
  val lineExpected = Rasterizer.rasterizeWithValue(line, re, magicNumber).toArray.filter(_ == magicNumber).length
  val squareExpected = Rasterizer.rasterizeWithValue(square, re, magicNumber).toArray.filter(_ == magicNumber).length
  val diamondExpected = Rasterizer.rasterizeWithValue(diamond, re, magicNumber).toArray.filter(_ == magicNumber).length
  val triangleExpected = Rasterizer.rasterizeWithValue(triangle, re, magicNumber).toArray.filter(_ == magicNumber).length

  val tile = re.foreachCell(e)({ (x,y) => 0 })
  val raster = Raster(tile, e)

  /*
   * Feature
   */
  describe("foreachCell methods on the Feature class") {
    it("should agree with Rasterizer.rasterizeWithValue for a point") {
      val actual1 = Feature[Point, Int](point, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Point, Int](point, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      val actual3 = Feature[Point, Double](point, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual4 = Feature[Point, Double](point, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      assert(actual1 == pointExpected)
      assert(actual2 == pointExpected)
      assert(actual3 == pointExpected)
      assert(actual4 == pointExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a line") {
      val actual1 = Feature[Line, Int](line, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Line, Int](line, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      val actual3 = Feature[Line, Double](line, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual4 = Feature[Line, Double](line, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      assert(actual1 == lineExpected)
      assert(actual2 == lineExpected)
      assert(actual3 == lineExpected)
      assert(actual4 == lineExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a polygon") {
      val actual1 = Feature[Polygon, Int](square, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Polygon, Int](square, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      val actual3 = Feature[Polygon, Double](square, magicNumber).foreachCell(re).toArray.filter(_ == magicNumber).length
      val actual4 = Feature[Polygon, Double](square, magicNumber).foreachCellDouble(re).toArray.filter(_ == magicNumber).length
      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
      assert(actual3 == squareExpected)
      assert(actual4 == squareExpected)
    }
  }

  /*
   * Geometry
   */
  describe("foreachCell methods on the Geometry class") {
    it("should agree with Rasterizer.rasterizeWithValue for a point") {
      val actual1 = point.foreachCell(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = point.foreachCellDouble(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == pointExpected)
      assert(actual2 == pointExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a line") {
      val actual1 = line.foreachCell(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = line.foreachCellDouble(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == lineExpected)
      assert(actual2 == lineExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a polygon") {
      val actual1 = square.foreachCell(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = square.foreachCellDouble(re)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }
  }

  /*
   * Tile
   */
  describe("foreachCell method on the Tile class") {
    it("should make use of function argument") {
      val actual1 = tile.foreachCell(square,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      val actual2 = tile.foreachCell(square,e)({ z => if (z == 1) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      assert(actual1 != 0)
      assert(actual2 == 0)
    }

    it("should agree with Rasterizer.rasterizeWithvalue for a square") {
      val actual1 = tile.foreachCell(square,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      val actual2 = tile.foreachCellDouble(square,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }

    it("should agree with Rasterizer.rasterizeWithvalue for a diamond") {
      val actual1 = tile.foreachCell(diamond,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      val actual2 = tile.foreachCellDouble(diamond,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      assert(actual1 == diamondExpected)
      assert(actual2 == diamondExpected)
    }

    it("should agree with Rasterizer.rasterizeWithvalue for a triangle") {
      val actual1 = tile.foreachCell(triangle,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      val actual2 = tile.foreachCellDouble(triangle,e)({ z => if (z == 0) magicNumber; else z }).toArray.filter(_ == magicNumber).length
      assert(actual1 == triangleExpected)
      assert(actual2 == triangleExpected)
    }
  }

  /*
   * Raster
   */
  describe("foreachCell method on the Raster class") {
    it("should agree with Rasterizer.rasterizeWithvalue for a square") {
      val actual1 = raster.foreachCell(square)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = raster.foreachCellDouble(square)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }

    it("should agree with Rasterizer.rasterizeWithvalue for a diamond") {
      val actual1 = raster.foreachCell(diamond)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = raster.foreachCellDouble(diamond)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == diamondExpected)
      assert(actual2 == diamondExpected)
    }

    it("should agree with Rasterizer.rasterizeWithvalue for a triangle") {
      val actual1 = raster.foreachCell(triangle)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = raster.foreachCellDouble(triangle)({ z => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == triangleExpected)
      assert(actual2 == triangleExpected)
    }
  }

  /*
   * RasterExtent
   */
  describe("foreachCell method on the RasterExtent class") {
    it("should agree with Rasterizer.rasterizeWithValue for a square") {
      val actual1 = re.foreachCell(square)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = re.foreachCellDouble(square)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a diamond") {
      val actual1 = re.foreachCell(diamond)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = re.foreachCellDouble(diamond)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val expected = Rasterizer.rasterizeWithValue(diamond, re, magicNumber).toArray.filter(_ == magicNumber).length
      assert(actual1 == diamondExpected)
      assert(actual2 == diamondExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a triangle") {
      val actual1 = re.foreachCell(triangle)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val actual2 = re.foreachCellDouble(triangle)({ (x,y) => magicNumber }).toArray.filter(_ == magicNumber).length
      val expected = Rasterizer.rasterizeWithValue(triangle, re, magicNumber).toArray.filter(_ == magicNumber).length
      assert(actual1 == triangleExpected)
      assert(actual2 == triangleExpected)
    }
  }

}
