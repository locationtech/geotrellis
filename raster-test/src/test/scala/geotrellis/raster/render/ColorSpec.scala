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

import org.scalatest._


import java.util.Locale

object ColorSpec {
  def hexstringify(colors: Seq[Int]) = colors.map{ "%08x".formatLocal(Locale.ENGLISH, _) }.toList
  def getColorString(colors: Seq[Int]) = colors.map("%06X".formatLocal(Locale.ENGLISH, _)).mkString(",")
}

class ColorSpec extends FunSpec with Matchers {
  describe("chooseColors") {
    val (color1, color2) = (0xFF0000, 0x0000FF)
    val colorArray = Vector(0xFF0000, 0x0000FF)

    def getColorStringLinear(numColors: Int) =
      ColorSpec.getColorString(ColorRamp(color1, color2).stops(numColors).colors)

    it("should provide 1 color") {
      getColorStringLinear(1) should be ("FF0000")
    }

    it("should provide 2 colors") {
      getColorStringLinear(2) should be ("FF0000,0000FF")
    }

    it("should provide 3 colors") {
      getColorStringLinear(3) should be ("FF0000,80007F,0000FF")
    }

    it("should provide 9 colors") {
      getColorStringLinear(9) should be ("FF0000,E0001F,C0003F,A0005F,80007F,60009F,4000BF,2000DF,0000FF")
    }

    it("should unzip colors") {
      val n = 0xff9900ff
      val (r, g, b, a) = n.unzip
      println(s"n=$n, r=$r g=$g b=$b a=$a")
      r should be (0xff)
      g should be (0x99)
      b should be (0x00)
      a should be (0xff)
    }
  }

  describe("MultiColorRangeChooser()") {
    def getColors(baseColors: Vector[Int], numColors: Int): Vector[Int] = {
      ColorRamp(baseColors).stops(numColors)
    }

    it("should work 1") {
      val baseColors = Vector(0x0000ffff)
      val colors = getColors(baseColors, baseColors.length)
      colors.toList should be (baseColors.toList)
    }

    it("should work 2") {
      val baseColors = Vector(0x0000ffff, 0xff0000ff)
      val colors = getColors(baseColors, baseColors.length)
      colors.toList should be (baseColors.toList)
    }

    it("should work 3") {
      val baseColors = Vector(0x0000ffff, 0x00ff00ff, 0xff0000ff)
      val colors = getColors(baseColors, baseColors.length)
      colors.map{ "%06x".format(_) }.toList should be (baseColors.map { "%06x".format(_) }.toList)
    }

    it("should work 6") {
      val baseColors = Vector(0x0000ffff, 0x0080ffff, 0x00ff80ff, 0xffff00ff, 0xff8000ff, 0xff0000ff)
      val colors = getColors(baseColors, baseColors.length)
      colors.toList should be (baseColors.toList)
    }

    it ("should interpolate") {
      val baseColors = Vector(0x0000ffff, 0xff0000ff)
      val expectedColors = Vector(0x0000ffff, 0x7f0080ff, 0xff0000ff)
      val colors = getColors(baseColors, 3)
      ColorSpec.hexstringify(colors) should be (ColorSpec.hexstringify(expectedColors))
    }

    it ("should interpolate 5 colors between 3 given") {
      val baseColors = Vector(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Vector(0xff0000ff, 0x807f00ff, 0x00ff00ff, 0x00807fff, 0x0000ffff)
      val colors = getColors(baseColors, 5)
      ColorSpec.hexstringify(colors) should be (ColorSpec.hexstringify(expected))
    }
  }

  describe("ColorMap") {
    it("should map breaks to colors") {
      val limits = Vector(2, 4, 6)
      val colors = Vector(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val scc = ColorMap(limits, colors)
      scc.colors should be (colors)
    }
  }

  describe("Blending Color Map") {
    it("should interpolate") {
      val colors = Vector(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Vector(0xff0000ff, 0x807f00ff, 0x00ff00ff, 0x00807fff, 0x0000ffff)
      val colorRamp = ColorRamp(colors).stops(5)
      ColorSpec.hexstringify(colorRamp.colors) should be (ColorSpec.hexstringify(expected))
    }
    it("should set alpha values") {
      val colors = Vector(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Vector(0xff000080, 0x00ff0080, 0x0000ff80)
      val colorRamp =
        ColorRamp(colors).setAlpha(0x80)
      ColorSpec.hexstringify(colorRamp.colors) should be (ColorSpec.hexstringify(expected))
    }

    it("should create an alpha gradient") {
      val colors = Vector(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Vector(0xff000000, 0x00ff007f, 0x0000ffff)
      val colorRamp =
        ColorRamp(colors).setAlphaGradient(0, 0xFF)
      ColorSpec.hexstringify(colorRamp.colors) should be (ColorSpec.hexstringify(expected))
    }
  }

  describe("RGBA value class") {
    it("should be able to create RGB values") {
      // an RGB constructor should create an RGBA with a fully opaque A
      RGB(1, 2, 3) should be (RGBA(1, 2, 3, 255))

      // we need to be able to convert from RGBA to ARGB for the current jpg writer implementation
      RGB(1, 2, 3) should be (RGBA(2, 3, 255, 1).toARGB)
    }

    it("should pick out individual colors") {
      val color = 0x11223344
      color.red should be (0x11)
      color.green should be (0x22)
      color.blue should be (0x33)
      color.alpha should be (0x44)
    }

    it("should 'unzip' to a tuple of colors") {
      val color = 0x11223344
      color.unzip should be (0x11, 0x22, 0x33, 0x44)
      color.unzipRGBA should be (0x11, 0x22, 0x33, 0x44)
      color.unzipRGB should be (0x11, 0x22, 0x33)
    }

    it("should have correct predicates") {
      val opaqueGrey = 0x222222ff
      opaqueGrey.isGrey should be (true)
      opaqueGrey.isOpaque should be (true)
      opaqueGrey.isTransparent should be (false)

      val transparentRed = 0xff000000
      transparentRed.isGrey should be (false)
      transparentRed.isOpaque should be (false)
      transparentRed.isTransparent should be (true)

      val spookyColor = 0x00000011
      spookyColor.isGrey should be (true)
      spookyColor.isOpaque should be (false)
      spookyColor.isTransparent should be (false)
    }
  }
}
