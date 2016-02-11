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

import org.scalatest._


import java.util.Locale

object ColorSpec {
  def hexstringify(colors:Array[Int]) = colors.map{ "%08x".formatLocal(Locale.ENGLISH, _) }.toList
  def getColorString(colors:Array[Int]) = colors.map("%06X".formatLocal(Locale.ENGLISH, _)).mkString(",")
}

class ColorSpec extends FunSpec with Matchers {
  describe("chooseColors") {
    val (color1,color2) = (RGBA(0xFF0000), RGBA(0x0000FF))
    val colorArray = Array(0xFF0000, 0x0000FF).map(RGBA(_))

    def getColorStringLinear(numColors:Int) = {
      val bicc = new BlendingIntColorClassifier
      val breaks = (1 to numColors).toArray
      bicc.addColors(color1, color2).addBreaks(breaks).normalize
      println("lengthsLinear", breaks.length, colorArray.length, bicc.length, bicc.getColors.length)
      ColorSpec.getColorString(bicc.getColors.map(_.get))
    }

    def getColorStringArray(numColors:Int) = {
      val bicc = new BlendingIntColorClassifier
      val breaks = (1 to numColors).toArray
      bicc.addColors(colorArray).addBreaks(breaks).normalize
      println("lengthsArray", breaks.length, colorArray.length, bicc.length, bicc.getColors.length)
      ColorSpec.getColorString(bicc.getColors.map(_.get))
    }

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
      println(s"n=$n, r=$r g=$g b=$b a=$a")
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

  describe("ColorClassifier") {
    it("should map breaks to colors") {
      val limits = Array(2, 4, 6)
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff).map(RGBA(_))
      val sicc = StrictColorClassifier(limits zip colors)
      sicc.getBreaks should be (limits)
      sicc.getColors should be (colors)
    }
  }

  describe("Blending Color Classifier") {
    it("should interpolate") {
      val colors = Array(RGBA(0xff0000ff), RGBA(0x00ff00ff), RGBA(0x0000ffff))
      val expected = Array(0xff0000ff, 0x807f00ff, 0x00ff00ff, 0x00807fff, 0x0000ffff)
      val bicc = new BlendingIntColorClassifier
      bicc.addColors(colors).addBreaks(1, 2, 3, 4, 5).normalize
      val interpolatedColors = bicc.getColors.map(_.get)
      println(interpolatedColors)
      ColorSpec.hexstringify(interpolatedColors) should be (ColorSpec.hexstringify(expected))
    }
    it("should set alpha values") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff).map(RGBA(_))
      val expected = Array(0xff000080, 0x00ff0080, 0x0000ff80)
      val bcc = new BlendingDoubleColorClassifier
      bcc.addColors(colors).setAlpha(0x80)
      ColorSpec.hexstringify(bcc.getColors.map(_.get)) should be (ColorSpec.hexstringify(expected))
    }

    it("should create an alpha gradient") {
      val colors = Array(0xff0000ff, 0x00ff00ff, 0x0000ffff).map(RGBA(_))
      val expected = Array(0xff000000, 0x00ff007f, 0x0000ffff)
      val bcc = new BlendingDoubleColorClassifier
      bcc.addColors(colors).alphaGradient(RGBA(0), RGBA(0xff))
      ColorSpec.hexstringify(bcc.getColors.map(_.get)) should be (ColorSpec.hexstringify(expected))
    }
  }
}
