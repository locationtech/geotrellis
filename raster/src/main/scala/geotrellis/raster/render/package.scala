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

package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  // RGB and RGBA
  // Note: GeoTrellis by default expects colors to be in RGBA format.

  implicit class RGBA(val int: Int) extends AnyVal {
    def red = (int >> 24) & 0xff
    def green = (int >> 16) & 0xff
    def blue = (int >> 8) & 0xff
    def alpha = int & 0xff
    def isOpaque = (alpha == 255)
    def isTransparent = (alpha == 0)
    def isGrey = (red == green) && (green == blue)
    def unzip = (red, green, blue, alpha)
    def toARGB = (int >> 8) | (alpha << 24)
    def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
    def unzipRGB: (Int, Int, Int) = (red, green, blue)
  }

  object RGB {
    def apply(r: Int, g: Int, b: Int): Int = ((r << 24) + (g << 16) + (b << 8)) | 0xFF
  }

  object RGBA {
    def apply(r: Int, g: Int, b: Int, a: Int): Int =
      new RGBA((r << 24) + (g << 16) + (b << 8) + a).int

    def apply(r: Int, g: Int, b: Int, alphaPct: Double): Int = {
      assert(0 <= alphaPct && alphaPct <= 100)
      RGBA(r, g, b, (alphaPct * 2.55).toInt)
    }
  }

  object HSV {
    private def convert(h: Double, s: Double, v: Double): (Double, Double, Double) = {
      def mod(d: Double, n: Int) = {
        val fraction = d - d.floor
        (d.floor.longValue % n).toDouble + fraction
      }

      val c  = s*v
      val h1 = h / 60.0
      val x  = c*(1.0 - ((mod(h1,  2)) - 1.0).abs)
      val (r,g,b) = if      (h1 < 1.0) (c, x, 0.0)
                    else if (h1 < 2.0) (x, c, 0.0)
                    else if (h1 < 3.0) (0.0, c, x)
                    else if (h1 < 4.0) (0.0, x, c)
                    else if (h1 < 5.0) (x, 0.0, c)
                    else  /*h1 < 6.0*/ (c, 0.0, x)
      val m = v-c
      (r+m, g+m, b+m)
    }

    def toRGB(h: Double, s: Double, v: Double): Int = {
      val (r, g, b) = convert(h, s, v)
      RGB((r*255).toInt, (g*255).toInt, (b*255).toInt)
    }

    def toRGBA(h: Double, s: Double, v: Double, a: Double): Int = {
      val (r, g, b) = convert(h, s, v)
      RGBA((r*255).toInt, (g*255).toInt, (b*255).toInt, (a*255).toInt)
    }
  }
}
