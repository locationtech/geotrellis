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

package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.render.png._
import geotrellis.raster.testkit._

import org.scalatest._

import java.io._
import java.awt.Color
import javax.imageio._

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

  describe("ColorMaps to-and-from Strings") {
    val s = "23:cc00ccff;30:aa00aaff;120:ff0000ff"
    val sD = "23.0:cc00ccff;30.0:aa00aaff;120.0:ff0000ff"

    it("fromString should parse correctly") {
      ColorMap.fromString(s) shouldBe defined
    }

    it("(int) fromString and breaksString should form an isomorphism") {
      ColorMap.fromString(s).get.breaksString shouldBe s
    }

    it("(double) fromString and breaksString should form an isomorphism") {
      ColorMap.fromStringDouble(sD).get.breaksString shouldBe sD
    }
  }

  describe("PNG Color Mapping") {
    it("should correctly map values to colors") {
      val limits = Array(25,50,80,100)
      val colors = Array(100,110,120,130)

      val colorMap1 =
        ColorMap(limits, colors)
      val colorMap = colorMap1.withNoDataColor(0).withBoundaryType(LessThanOrEqualTo)
      val arr = (0 until 90 by 5).toArray
      val r = createTile(arr)

      val color: IndexedPngEncoding =
        PngColorEncoding(colorMap.colors, colorMap.options.noDataColor, colorMap.options.fallbackColor) match {
          case i @ IndexedPngEncoding(_, _) => i
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

    it("should correctly map values to colors using IntCacheColorMap") {
      val limits = Array(25,50,80,100)
      val colors = Array(100,110,120,130)

      val colorRamp: ColorRamp = colors
      val breaksToColors: Map[Int, Int] = (limits zip colorRamp.stops(limits.size).colors).toMap
      val colorMap1 = new IntColorMap(breaksToColors, ColorMap.Options(noDataColor =  0, classBoundaryType = LessThanOrEqualTo))
      val arr = (0 until 90 by 5).toArray
      val r = createTile(arr)
      val h = r.histogram
      val colorMap = colorMap1.cache(h)

      val color: IndexedPngEncoding =
        PngColorEncoding(colorMap.colors, colorMap.options.noDataColor, colorMap.options.fallbackColor) match {
          case i @ IndexedPngEncoding(_, _) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      // check that PNG will correctly convert raster to their color index values
      val pngMap = color.convertColorMap(colorMap)
      h.foreachValue { z =>
        val color = colorMap.map(z)
        val colorIndex = colorMap.colors.indexOf(color)
        pngMap.map(z) should be(colorIndex)
      }

      // check that we can convert all raster values we have seen in our histogram
      withClue(s"Cached colors: ${colorMap.colors.toList}") {
        for (x <- arr) {
          if (x <= 25) colorMap.map(x) should be(100)
          else if (x <= 50) colorMap.map(x) should be(110)
            // tile does not have any values past 75, since we didn't see them, we don't expect to color them
          else if (x > 75) colorMap.options.noDataColor
        }
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
        PngColorEncoding(colorMap.colors, colorMap.options.noDataColor, colorMap.options.noDataColor) match {
          case i @ IndexedPngEncoding(_, _) => i
          case _ =>
            withClue(s"Color should be Indexed") { sys.error("") }
        }

      colorMap.map(10) should be (10)
      colorMap.map(20) should be (10)
      colorMap.map(30) should be (20)
      colorMap.map(40) should be (20)
      colorMap.map(50) should be (30)
    }

    it("should render transparent pixels for nodata and unmapped values on Double tiles"){
      val arr = Array[Double](Double.NaN, 2.0, 9000.0)
      val tile = DoubleArrayTile(arr, 3, 1)
      val cmap = ColorMap(Map[Double, Int](3.0 -> 0xFF0000FF))
      val png = tile.renderPng(cmap)

      val img = ImageIO.read(new ByteArrayInputStream(png.bytes))

      val nd = new Color(img.getRGB(0, 0), true)
      nd.getAlpha should be (0)

      val fallback = new Color(img.getRGB(2, 0), true)
      fallback.getAlpha should be (0)
    }

    it("should render transparent pixels for nodata and unmapped values on Int tiles"){
      val arr = Array[Int](Int.MinValue, 2, 9000)
      val tile = IntArrayTile(arr, 3, 1)
      val cmap = ColorMap(Map[Int, Int](3 -> 0xFF0000FF))
      val png = tile.renderPng(cmap)

      val img = ImageIO.read(new ByteArrayInputStream(png.bytes))

      val nd = new Color(img.getRGB(0, 0), true)
      nd.getAlpha should be (0)

      val fallback = new Color(img.getRGB(2, 0), true)
      fallback.getAlpha should be (0)
    }
  }

  describe("ColorMap class boundary type option") {

    it("should map LessThan correctly on class boundary borders - int") {
      val colorMap =
        ColorMap(
          Map(
            19 -> 0xFF0000FF,
            23 -> 0x00FF00FF,
            26 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = LessThan)
        )

      val color = colorMap.map(23)
      withClue(f"Actual color = ${color}%02X: - int") {
        color should be (0x0000FFFF)
      }
    }

    it("should map LessThanOrEqualTo correctly on class boundary borders - int") {
      val colorMap =
        ColorMap(
          Map(
            19 -> 0xFF0000FF,
            23 -> 0x00FF00FF,
            26 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = LessThanOrEqualTo)
        )

      val color = colorMap.map(23)
      withClue(f"Actual color = ${color}%02X: - int") {
        color should be (0x00FF00FF)
      }
    }

    it("should map GreaterThan correctly on class boundary borders - int") {
      val colorMap =
        ColorMap(
          Map(
            19 -> 0xFF0000FF,
            23 -> 0x00FF00FF,
            26 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = GreaterThan)
        )

      val color = colorMap.map(23)
      withClue(f"Actual color = ${color}%02X: - int") {
        color should be (0xFF0000FF)
      }
    }

    it("should map GreaterThanOrEqualTo correctly on class boundary borders - int") {
      val colorMap =
        ColorMap(
          Map(
            19 -> 0xFF0000FF,
            23 -> 0x00FF00FF,
            26 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = GreaterThanOrEqualTo)
        )

      val color = colorMap.map(23)
      withClue(f"Actual color = ${color}%02X:") {
        color should be (0x00FF00FF)
      }
    }

    it("should map LessThan correctly on class boundary borders - double") {
      val colorMap =
        ColorMap(
          Map(
            19.5 -> 0xFF0000FF,
            23.5 -> 0x00FF00FF,
            26.5 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = LessThan)
        )

      val color = colorMap.mapDouble(23.5)
      withClue(f"Actual color = ${color}%02X: - double") {
        color should be (0x0000FFFF)
      }
    }

    it("should map LessThanOrEqualTo correctly on class boundary borders - double") {
      val colorMap =
        ColorMap(
          Map(
            19.5 -> 0xFF0000FF,
            23.5 -> 0x00FF00FF,
            26.5 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = LessThanOrEqualTo)
        )

      val color = colorMap.mapDouble(23.5)
      withClue(f"Actual color = ${color}%02X: - double") {
        color should be (0x00FF00FF)
      }
    }

    it("should map GreaterThan correctly on class boundary borders - double") {
      val colorMap =
        ColorMap(
          Map(
            19.5 -> 0xFF0000FF,
            23.5 -> 0x00FF00FF,
            26.5 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = GreaterThan)
        )

      val color = colorMap.mapDouble(23.5)
      withClue(f"Actual color = ${color}%02X: - double") {
        color should be (0xFF0000FF)
      }
    }

    it("should map GreaterThanOrEqualTo correctly on class boundary borders - double") {
      val colorMap =
        ColorMap(
          Map(
            19.5 -> 0xFF0000FF,
            23.5 -> 0x00FF00FF,
            26.5 -> 0x0000FFFF
          ),
          ColorMap.Options(classBoundaryType = GreaterThanOrEqualTo)
        )

      val color = colorMap.mapDouble(23.5)
      withClue(f"Actual color = ${color}%02X:") {
        color should be (0x00FF00FF)
      }
    }
  }
}
