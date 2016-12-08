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
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          if (col == 1 && row == 1) actual = p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      actual should be (expected)
    }

    it("should correctly not report disjoint pixels") {
      val poly = Polygon(Point(1,1), Point(1,2), Point(2,2), Point(2,1), Point(1,1))
      var actual = 0.0
      val expected = 0.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          if (col != 1 && row != 1) actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      actual should be (expected)
    }

    it("should correctly report partial pixels") {
      val poly = Polygon(Point(1,1), Point(1,2), Point(2,2), Point(1,1))
      var actual = Double.NaN
      val expected = 0.5
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          if (col == 1 && row == 1) actual = p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      actual should be (expected)
    }

    it("should handle sub-pixel activity") {
      val poly = Polygon(Point(1.2,1.2), Point(1.2,1.8), Point(1.8,1.8), Point(1.8,1.2), Point(1.2,1.2))
      var actual = Double.NaN
      val expected = 0.36
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          if (col == 1 && row == 1) actual = p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a mix of partial and complete pixels") {
      val poly = Polygon(Point(0.1,0.1), Point(0.1,2.9), Point(2.9,2.9), Point(2.9,0.1), Point(0.1,0.1))
      var actual = 0.0
      val expected = 2.8 * 2.8
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (1/6)") {
      val re = RasterExtent(e, 30, 30)
      val poly = Polygon(Point(0,0), Point(3,0), Point(3,3), Point(0,0))
      var actual = 0.0
      val expected = (30*30)/2.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (2/6)") {
      val re = RasterExtent(e, 30, 30)
      val poly = Polygon(Point(0,0), Point(3,0), Point(0,3), Point(0,0))
      var actual = 0.0
      val expected = (30*30)/2.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (3/6)") {
      val re = RasterExtent(Extent(0, 0, 3, 3.1), 30, 30)
      val poly = Polygon(Point(0,0), Point(3,0), Point(0,3), Point(0,0))
      var actual = 0.0
      val expected = 435.483871
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (4/6)") {
      val re = RasterExtent(Extent(0, 0, 3.1, 3), 30, 30)
      val poly = Polygon(Point(0,0), Point(3,0), Point(0,3), Point(0,0))
      var actual = 0.0
      val expected = 435.483871
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (5/6)") {
      val re = RasterExtent(Extent(0, 0, 3, 3.1), 30, 30)
      val poly = Polygon(Point(0,0), Point(3,0), Point(0,3.1), Point(0,0))
      var actual = 0.0
      val expected = (30*30)/2.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle a long diagonal line (6/6)") {
      val re = RasterExtent(Extent(0, 0, 3.1, 3), 30, 30)
      val poly = Polygon(Point(0,0), Point(3.1,0), Point(0,3), Point(0,0))
      var actual = 0.0
      val expected = (30*30)/2.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByPolygon(poly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

  }

  describe("Fractional-Pixel MultiPolygon Rasterizer") {
    val e = Extent(0, 0, 3, 3)
    val re = RasterExtent(e, 3, 3)

    val e2 = Extent(0, 0, 9, 3)
    val re2 = RasterExtent(e2, 9, 3)

    it("should behave like the Polygon rasterizer for one-component MultiPolygons") {
      val re = RasterExtent(Extent(0, 0, 3.1, 3), 30, 30)
      val multipoly = MultiPolygon(Polygon(Point(0,0), Point(3.1,0), Point(0,3), Point(0,0)))
      var actual = 0.0
      val expected = (30*30)/2.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByMultiPolygon(multipoly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle MultiPolygons w/ pixel-disjoint components") {
      val multipoly = MultiPolygon(
        Polygon(Point(0,0), Point(3,1.5), Point(0, 3), Point(0, 0)),
        Polygon(Point(9,0), Point(6,1.5), Point(9, 3), Point(9, 0)))
      var actual = 0.0
      val expected = 9.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByMultiPolygon(multipoly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle MultiPolygons w/ disjoint components") {
      val multipoly = MultiPolygon(
        Polygon(Point(1.4,0), Point(4.4,1.5), Point(1.4, 3), Point(1.4, 0)),
        Polygon(Point(7.6,0), Point(4.6,1.5), Point(7.6, 3), Point(7.6, 0)))
      var actual = 0.0
      val expected = 9.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByMultiPolygon(multipoly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

    it("should handle MultiPolygons w/ components that share a point") {
      val multipoly = MultiPolygon(
        Polygon(Point(1.5,0), Point(4.5,1.5), Point(1.5, 3), Point(1.5, 0)),
        Polygon(Point(7.5,0), Point(4.5,1.5), Point(7.5, 3), Point(7.5, 0)))
      var actual = 0.0
      val expected = 9.0
      val cb = new FractionCallback {
        def callback(col: Int, row: Int, p: Double): Unit =
          actual += p
      }

      FractionalRasterizer.foreachCellByMultiPolygon(multipoly, re)(cb)

      round(actual * 1000000) should be (round(expected * 1000000))
    }

}

}
