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

package geotrellis.raster.rasterize

import geotrellis.raster._
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

  val raster = re.rasterize(e)({ (x,y) => 0 })
  val tile = raster.tile

  /*
   * Feature
   */
  describe("The rasterize methods on the Feature class") {
    it("should agree with Rasterizer.rasterizeWithValue for a point") {
      val actual1 = Feature[Point, Int](point, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Point, Double](point, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == pointExpected)
      assert(actual2 == pointExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a line") {
      val actual1 = Feature[Line, Int](line, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Line, Double](line, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == lineExpected)
      assert(actual2 == lineExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a polygon") {
      val actual1 = Feature[Polygon, Int](square, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = Feature[Polygon, Double](square, magicNumber).rasterize(re)
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }
  }

  /*
   * Geometry
   */
  describe("The rasterize methods on the Geometry class") {
    it("should agree with Rasterizer.rasterizeWithValue for a point") {
      val actual1 = point.rasterize(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = point.rasterizeDouble(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == pointExpected)
      assert(actual2 == pointExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a line") {
      val actual1 = line.rasterize(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = line.rasterizeDouble(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == lineExpected)
      assert(actual2 == lineExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a polygon") {
      val actual1 = square.rasterize(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = square.rasterizeDouble(re)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }
  }

  /*
   * RasterExtent
   */
  describe("The rasterize and foreach methods on the RasterExtent class") {
    it("should agree with Rasterizer.rasterizeWithValue for a square") {
      val actual1 = re.rasterize(square)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = re.rasterizeDouble(square)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length

      assert(actual1 == squareExpected)
      assert(actual2 == squareExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a diamond") {
      val actual1 = re.rasterize(diamond)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = re.rasterizeDouble(diamond)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val expected = Rasterizer.rasterizeWithValue(diamond, re, magicNumber)
        .toArray.filter(_ == magicNumber).length

      assert(actual1 == diamondExpected)
      assert(actual2 == diamondExpected)
    }

    it("should agree with Rasterizer.rasterizeWithValue for a triangle") {
      val actual1 = re.rasterize(triangle)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val actual2 = re.rasterizeDouble(triangle)({ (x: Int, y: Int) => magicNumber })
        .tile.toArray.filter(_ == magicNumber).length
      val expected = Rasterizer.rasterizeWithValue(triangle, re, magicNumber)
        .toArray.filter(_ == magicNumber).length

      assert(actual1 == triangleExpected)
      assert(actual2 == triangleExpected)
    }
  }

}
