/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.render.png._
import geotrellis.raster.testkit._

import org.scalatest._

class ColorMapSpec extends FunSpec with Matchers
                                   with TileBuilders {
  describe("color map construction") {
    it("should classify ints to colors") {
      val colorMap =
        ColorMap(
          123 -> 123,
          1234 -> 1234,
          1235 -> 1235,
          1236 -> 1236
        ).withNoDataColor(8675309)

      colorMap.colors shouldBe (Array(123, 1234, 1235, 1236))
      colorMap.options.noDataColor shouldBe (8675309)
    }

    it("should classify doubles to colors") {
      val colorMap =
        ColorMap(
          123.23 -> 123,
          12234.89 -> 1234,
          45.342 -> 1235,
          1236.13 -> 1236
        ).withNoDataColor(8675309)
      colorMap.colors shouldBe (Array(1235, 123, 1236, 1234))
      colorMap.options.noDataColor shouldBe (8675309)
    }
  }

  describe("color map creation") {
    it("should build a color map with fully specifiable options") {
      val ndColor = RGBA(0, 0, 0, 100.0)
      val fallbackColor = RGBA(255, 0, 0, 0)
      val colorMap =
        ColorMap((0, 1))
          .withNoDataColor(ndColor)
          .withFallbackColor(fallbackColor)
          .withBoundaryType(Exact)

      colorMap.options shouldBe (ColorMap.Options(Exact, ndColor.int, fallbackColor.int, false))
    }
  }

  describe("PNG Color Mapping") {
    it("should correctly map values to colors") {
      val limits = Array(25,50,80,100)
      val colors = Array(100,110,120,130)

      val colorMap1 =
        ColorMap(limits, colors)
      val colorMap = colorMap1.withNoDataColor(0)
      val arr = (0 until 90 by 5).toArray
      val r = createTile(arr)

      val color: IndexedPngEncoding =
        PngColorEncoding(colorMap.colors, colorMap.options.noDataColor) match {
          case i @ IndexedPngEncoding(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      for(x <- arr) {
        if(x <= 25) colorMap.map(x) should be (100)
        else if(x <= 50) colorMap.map(x) should be (110)
        else if (x <= 80) colorMap.map(x) should be (120)
        else { colorMap.map(x) should be (130) }
      }
    }

    it("should correctly map redundant values to colors") {
      val limits = Array(25,42,60)
      val colors = Array(10,20,30)
      val colorMap =
        ColorMap(limits, colors).withNoDataColor(0)

      val arr = Array(10,10,10,10,10,10,10,20,20,20,20,30,30,30,40,50)
      val r = createTile(arr)

      val color: IndexedPngEncoding =
        PngColorEncoding(colorMap.colors, colorMap.options.noDataColor) match {
          case i @ IndexedPngEncoding(_,_) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      colorMap.map(10) should be (10)
      colorMap.map(20) should be (10)
      colorMap.map(30) should be (20)
      colorMap.map(40) should be (20)
      colorMap.map(50) should be (30)
    }
  }
}
