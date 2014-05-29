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

package geotrellis.render

import org.scalatest._

object ColorSpec {
  def hexstringify(colors:Array[Int]) = colors.map{ "%08x".format(_) }.toList 
  def getColorString(colors:Array[Int]) = colors.map("%06X" format _).mkString(",")
}

class ColorSpec extends FunSpec with Matchers {
  describe("chooseColors") {
    val (color1,color2) = (0xFF0000, 0x0000FF)
    val colorArray = Array(0xFF0000, 0x0000FF)

    def getColorStringLinear(numColors:Int) =
      ColorSpec.getColorString(Color.chooseColors(color1,color2,numColors))

    def getColorStringArray(numColors:Int) = 
      ColorSpec.getColorString(Color.chooseColors(colorArray,numColors))

    it("should provide 1 color") {
      getColorStringLinear(1) should be ("FF0000")
      getColorStringArray(1) should be ("FF0000")
    }

    it("should provide 2 colors") {
      getColorStringLinear(2) should be ("FF0000,0000FF")
      getColorStringArray(2) should be ("FF0000,0000FF")
    }

    it("should provide 3 colors") {
      getColorStringLinear(3) should be ("FF0000,80007F,0000FF")
      getColorStringArray(3) should be ("FF0000,80007F,0000FF")
    }

    it("should provide 9 colors") {
      getColorStringLinear(9) should be ("FF0000,E0001F,C0003F,A0005F,80007F,60009F,4000BF,2000DF,0000FF")
      getColorStringArray(9) should be ("FF0000,E0001F,C0003F,A0005F,80007F,60009F,4000BF,2000DF,0000FF")
    }

    it("should unzip colors") {
      val n = 0xff9900ff
      val (r, g, b, a) = Color.unzip(n)
      println("n=%s, r=%s g=%s b=%s a=%s" format (n, r, g, b, a))
      r should be (0xff)
      g should be (0x99)
      b should be (0x00)
      a should be (0xff)
    }
  }

  describe("MultiColorRangeChooser()") {
    def getColors(baseColors:Array[Int], numColors:Int) = 
      Color.chooseColors(baseColors,numColors)

    it("should work 1") {
      val baseColors = Array(0x0000ffff)
      val colors = getColors(baseColors,baseColors.length)
      colors.toList should be (baseColors.toList)
    }

    it("should work 2") {
      val baseColors = Array(0x0000ffff, 0xff0000ff)
      val colors = getColors(baseColors,baseColors.length)      
      colors.toList should be (baseColors.toList)
    }

    it("should work 3") {
      val baseColors = Array(0x0000ffff, 0x00ff00ff, 0xff0000ff)
      val colors = getColors(baseColors,baseColors.length)
      colors.map{ "%06x".format(_) }.toList should be (baseColors.map { "%06x".format(_) }.toList)
    }

    it("should work 6") {
      val baseColors = Array(0x0000ffff, 0x0080ffff, 0x00ff80ff, 0xffff00ff, 0xff8000ff, 0xff0000ff)
      val colors = getColors(baseColors,baseColors.length)
      colors.toList should be (baseColors.toList)
    }
    
    it ("should interpolate") {
      val baseColors = Array(0x0000ffff, 0xff0000ff)
      val expectedColors = Array(0x0000ffff, 0x7f0080ff, 0xff0000ff)
      val colors = getColors(baseColors,3)
      ColorSpec.hexstringify(colors) should be (ColorSpec.hexstringify(expectedColors))
    }
    
    it ("should interpolate 5 colors between 3 given") {
      val baseColors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Array(0xff0000ff, 0x807f00ff, 0x00ff00ff, 0x00807fff, 0x0000ffff)
      val colors = getColors(baseColors, 5)
      ColorSpec.hexstringify(colors) should be (ColorSpec.hexstringify(expected))
    }
  }

  describe("ColorBreaks") {
    it("should map breaks to colors") {
      val limits = Array(2, 4, 6)
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val cb = ColorBreaks(limits, colors)
      cb.limits should be (limits)
      cb.colors should be (colors)
    }
  }
  
  describe("ColorRamp") {
    it("should return the correct colors") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)

    }
    it("should interpolate") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Array(0xff0000ff, 0x807f00ff, 0x00ff00ff, 0x00807fff, 0x0000ffff)
      val interpolatedColors = ColorRamp(colors).interpolate(5)
      println(interpolatedColors.colors)
      ColorSpec.hexstringify(interpolatedColors.toArray) should be (ColorSpec.hexstringify(expected))
    }
    it("should set alpha values") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Array(0xff000080, 0x00ff0080, 0x0000ff80)
      val interpolatedColors = ColorRamp(colors).setAlpha(0x80)
      println(interpolatedColors.colors)
      ColorSpec.hexstringify(interpolatedColors.toArray) should be (ColorSpec.hexstringify(expected))
    }
    
    it("should create an alpha gradient") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff)
      val expected = Array(0xff000000, 0x00ff007f, 0x0000ffff)
      val interpolatedColors = ColorRamp(colors).alphaGradient(0, 0xff)
      ColorSpec.hexstringify(interpolatedColors.toArray) should be (ColorSpec.hexstringify(expected))
    }
  }
}
