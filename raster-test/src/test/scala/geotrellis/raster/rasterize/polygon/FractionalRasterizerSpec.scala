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

package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.vector._

import org.scalatest.{FunSpec, Matchers}

import scala.math.round


class FractionalRasterizerSpec extends FunSpec with Matchers {

  describe("Fractional-Pixel Polygon Rasterizer") {
    val e = Extent(0, 0, 3, 3)
    val re = RasterExtent(e, 3, 3)

    it("should correctly report full pixels") {
      val poly = Polygon(Point(1,1), Point(1,2), Point(2,2), Point(2,1), Point(1,1))
      var actual = Double.NaN
      val expected = 1.0

      FractionalRasterizer.foreachCellByPolygon(poly, re) { (col: Int, row: Int, p: Double) =>
        if (col == 1 && row == 1) actual = p
      }

      actual should be (expected)
    }

    it("should correctly not report disjoint pixels") {
      val poly = Polygon(Point(1,1), Point(1,2), Point(2,2), Point(2,1), Point(1,1))
      var actual = 0.0
      val expected = 0.0

      FractionalRasterizer.foreachCellByPolygon(poly, re) { (col: Int, row: Int, p: Double) =>
        if (col != 1 && row != 1) actual += p
      }

      actual should be (expected)
    }

    it("should correctly report partial pixels") {
      val poly = Polygon(Point(1,1), Point(1,2), Point(2,2), Point(1,1))
      var actual = Double.NaN
      val expected = 0.5

      FractionalRasterizer.foreachCellByPolygon(poly, re) { (col: Int, row: Int, p: Double) =>
        if (col == 1 && row == 1) actual = p
      }

      actual should be (expected)
    }

    it("should handle sub-pixel activity") {
      val poly = Polygon(Point(1.2,1.2), Point(1.2,1.8), Point(1.8,1.8), Point(1.8,1.2), Point(1.2,1.2))
      var actual = Double.NaN
      val expected = 0.36

      FractionalRasterizer.foreachCellByPolygon(poly, re) { (col: Int, row: Int, p: Double) =>
        if (col == 1 && row == 1) actual = p
      }

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a mix of partial and complete pixels") {
      val poly = Polygon(Point(0.1,0.1), Point(0.1,2.9), Point(2.9,2.9), Point(2.9,0.1), Point(0.1,0.1))
      var actual = 0.0
      val expected = 2.8 * 2.8

      FractionalRasterizer.foreachCellByPolygon(poly, re) { (col: Int, row: Int, p: Double) =>
        actual += p
      }

      round(actual * 1000000) should be (round(expected * 1000000))
    }

  }
}
