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

import java.util.Locale

/**
 * ColorBreaks describes a way to render a raster into a colored image.
 *
 * This class defines a set of value ranges and assigns a color to
 * each value range.
 *
 * @param limits  An array with the maximum value of each range
 * @param colors  An array with the color assigned to each range
 */
case class ColorBreaks(limits:Array[Int], colors:Array[Int]) {
  assert(limits.length == colors.length)
  assert(colors.length > 0)

  val lastColor = colors(colors.length - 1)

  def length = limits.length

  def get(z:Int):Int = {
    var i = 0
    val last = colors.length - 1
    while (i < last) {
      if (z <= limits(i)) return colors(i)
      i += 1
    }
    lastColor
  }

  override def toString =
    s"""ColorBreaks(${limits.mkString("Array(", ", ", ")")}, ${colors.map("%08x" formatLocal(Locale.ENGLISH, _)).mkString("Array(", ", ", ")")}"""
}

object ColorBreaks {
  /**
   * This method is used for cases in which we are provided with a different
   * number of colors than we have value ranges.  This method will return a
   * return a ClassBreak object where the provided colors are spaced out amongst
   * the ranges that exist.
   *
   * For example, if we are provided a list of 9 colors on a red
   * to green gradient, but only have three maximum values for 3 value ranges,
   * we expect to get back a ColorBreaks object with three ranges and three colors,
   * with the first being red, the second color being the 5th
   * color (between red and green), and the last being green.
   *
   * @param limits  An array of the maximum value of each range
   * @param colors  An array of RGBA color values
   */
  def assign(limits:Array[Int], colors:Array[Int]) = {
    if (limits.length != colors.length) {
      val used = new Array[Int](limits.length)
      used(0) = colors(0)

      val b = limits.length - 1
      val c = colors.length - 1
      var i = 1
      while (i < limits.length) {
        used(i) = colors(math.round(i.toDouble * c / b).toInt)
        i += 1
      }

      new ColorBreaks(limits, used)
    } else {
      new ColorBreaks(limits, colors)
    }
  }
}
