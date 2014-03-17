/**************************************************************************
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
 **************************************************************************/

package geotrellis.render

import geotrellis._

/**
 * All colors in geotrellis are encoded as RGBA integer values.
 *
 * This object provides utility methods that operate on RGBA integer values.
 */ 
object Color {
  // Get red color band from RGBA color value.
  @inline final def unzipR(x:Int) = (x >> 24) & 0xff

  // Get green color band from RGBA color value.
  @inline final def unzipG(x:Int) = (x >> 16) & 0xff

  // Get blue color band from RGBA color value.
  @inline final def unzipB(x:Int) = (x >> 8) & 0xff

  // Get alpha band from RGBA color balue.
  @inline final def unzipA(x:Int) = x & 0xff

  // Returns true if the alpha of this color is 255 (opaque).
  @inline final def isOpaque(x:Int) = unzipA(x) == 255
  
  // Returns true if the alpha of this color is 0 (100% transparent).
  @inline final def isTransparent(x:Int) = unzipA(x) == 0

  // Returns true if red, blue, and green band are equal.
  @inline final def isGrey(x:Int) = {
    Color.unzipR(x) == Color.unzipG(x) && Color.unzipG(x) == Color.unzipB(x)
  }

  // split one color value into three color band values plus an alpha band
  final def unzip(x:Int) = (unzipR(x), unzipG(x), unzipB(x), unzipA(x))

  // combine three color bands into one color value
  @inline final def zip(r:Int, g:Int, b:Int, a:Int) = (r << 24) + (g << 16) + (b << 8) + a

  // convert an RGB color integer to an opaque RGBA color integer
  def rgbToRgba(rgb:Int) = { (rgb << 8) + 0xff }

  /** Parses a color in hex string form */
  def parseColor(s:String) = (Integer.parseInt(s,16) << 8) | 0xff

  /**
   * This method is used for cases in which we are provided with a different
   * number of colors than we need.  This method will return a smaller list
   * of colors the provided list of colors, spaced out amongst the provided
   * color list.  
   *
   * For example, if we are provided a list of 9 colors on a red
   * to green gradient, but only need a list of 3, we expect to get back a 
   * list of 3 colors with the first being red, the second color being the 5th
   * color (between red and green), and the last being green.
   *
   * @param colors  Provided RGBA color values
   * @param n       Length of list to return 
   */
  def spread(colors:Array[Int], n:Int): Array[Int] = {
    if (colors.length == n) return colors

    val colors2 = new Array[Int](n)
    colors2(0) = colors(0)
  
    val b = n - 1
    val c = colors.length - 1
    var i = 1
    while (i < n) {
      colors2(i) = colors(math.round(i.toDouble * c / b).toInt)
      i += 1
    }

    colors2
  }

  /**
    * Interpolate value for individual color band (0-255).  
    */
  def blend(start:Int, end:Int, numerator:Int, denominator:Int) = { 
    start + (((end - start) * numerator) / denominator)
  }

  def chooseColors(colors: Array[Int], numColors: Int): Array[Int] =
    getColors(numColors) { (masker:Int => Int, count:Int) =>
      val hues = colors.map(masker)
      val mult = colors.length - 1
      val denom = count - 1

      if (count < 2) {
        Array(hues(0))
      } else {
        val ranges = new Array[Int](count)
        var i = 0
        while (i < count) {
          val j = (i * mult) / denom
          ranges(i) = if (j < mult) {
            blend(hues(j), hues(j + 1), (i * mult) % denom, denom)
            
          } else {
            hues(j)
          }
          i += 1
        }
        ranges
      }
    }

  def chooseColors(color1:Int, color2:Int, numColors:Int): Array[Int] =
    getColors(numColors) { (masker: Int => Int, count: Int) =>
      val start = masker(color1)
      val end   = masker(color2)
      if (numColors < 2) {
        Array(start)
      } else {
        val ranges = new Array[Int](numColors)
        var i = 0
        while (i < numColors) {
          ranges(i) = blend(start, end, i, numColors - 1)
          i += 1
        }
        ranges
      }
    }

  /** Returns a sequence of RGBA integer values */
  def getColors(n:Int)(getRanges:(Int => Int, Int) => Array[Int]):Array[Int] = {
    val rs = getRanges(Color.unzipR, n)
    val gs = getRanges(Color.unzipG, n)
    val bs = getRanges(Color.unzipB, n)
    val as = getRanges(Color.unzipA, n)

    val colors = new Array[Int](n)
    var i = 0
    while (i < n) {
      colors(i) = Color.zip(rs(i), gs(i), bs(i), as(i))
      i += 1
    }
    colors
  }
}
