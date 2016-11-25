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

package geotrellis.raster.rasterize.extent

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.raster.testkit._

import math.{max,min,round}

import org.scalatest.FunSuite

class ExtentRasterizerSpec extends FunSuite
    with TileBuilders {

  test("Rasterization of a covering Extent") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 1.0, 1.0, 10, 10)
    var sum = 0
    ExtentRasterizer.foreachCellByExtent(e, re) { (x : Int, y : Int) => sum = sum + 1 }
    assert(sum == 100)
  }

  test("Partially-covering extent w/ non-point pixels, w/ partial cells") {
    val e = Extent(0.51, 0.51, 9.49, 9.49)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 1.0, 1.0, 10, 10)
    val options = Options(includePartial = true, sampleType = PixelIsArea)

    var sum = 0
    ExtentRasterizer.foreachCellByExtent(e, re, options) { (x : Int, y : Int) => sum = sum + 1 }
    assert(sum == 100)
  }

  test("Partially-covering extent w/ point pixels, w/o partial cells") {
    val e = Extent(0.51, 0.51, 9.49, 9.49)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 1.0, 1.0, 10, 10)
    var sum = 0
    ExtentRasterizer.foreachCellByExtent(e, re) { (x : Int, y : Int) => sum = sum + 1 }
    assert(sum == 64)
  }

  test("Rasterization of non-square pixels w/ partial cells") {
    val e = Extent(1.01, 1.01, 8.99, 8.89)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 2.0, 2.0, 10, 10)
    val options = Options(includePartial = true, sampleType = PixelIsArea)

    var sum = 0
    ExtentRasterizer.foreachCellByExtent(e, re, options) { (x : Int, y : Int) => sum = sum + 1 }
    assert(sum == 25)
  }

  test("Rasterization of non-square pixels w/o partial cells") {
    val e = Extent(1.01, 1.01, 8.99, 8.89)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 2.0, 2.0, 10, 10)
    var sum = 0
    ExtentRasterizer.foreachCellByExtent(e, re) { (x : Int, y : Int) => sum = sum + 1 }
    assert(sum == 9)
  }
}

class ExtentPolygonXCheckSpec extends FunSuite
    with TileBuilders {

  test("Cross-Check with Polygon Rasterizer and 1x1 Pixels") {
    val e = Extent(0.51, 0.51, 9.49, 9.49)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 1.0, 1.0, 10, 10)
    val options1 = Options(includePartial = false, sampleType = PixelIsArea)
    val options2 = Options(includePartial = true, sampleType = PixelIsArea)
    var extentSum = 0
    var polySum = 0

    ExtentRasterizer.foreachCellByExtent(e, re, options1) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options1) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )

    ExtentRasterizer.foreachCellByExtent(e, re, options2) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options2) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )
  }

  test("Cross-Check with Polygon Rasterizer and 2x2 Pixels") {
    val e = Extent(1.01, 1.01, 8.99, 8.89)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 2.0, 2.0, 10, 10)
    val options1 = Options(includePartial = false, sampleType = PixelIsArea)
    val options2 = Options(includePartial = true, sampleType = PixelIsArea)
    var extentSum = 0
    var polySum = 0

    ExtentRasterizer.foreachCellByExtent(e, re, options1) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options1) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )

    ExtentRasterizer.foreachCellByExtent(e, re, options2) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options2) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )
  }

  test("Cross-Check with Polygon Rasterizer and 3x2 Pixels") {
    val e = Extent(1.01, 1.01, 8.99, 8.89)
    val re = RasterExtent(Extent(0.0, 0.0, 10.0, 10.0), 3.0, 2.0, 10, 10)
    val options1 = Options(includePartial = false, sampleType = PixelIsArea)
    val options2 = Options(includePartial = true, sampleType = PixelIsArea)
    var extentSum = 0
    var polySum = 0

    ExtentRasterizer.foreachCellByExtent(e, re, options1) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options1) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )

    ExtentRasterizer.foreachCellByExtent(e, re, options2) { (x : Int, y : Int) => extentSum = extentSum + 1 }
    PolygonRasterizer.foreachCellByPolygon(e, re, options2) { (x : Int, y : Int) => polySum = polySum + 1 }
    assert( extentSum == polySum )
  }
}
