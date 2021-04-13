/*
 * Copyright 2021 Azavea
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
    RGBA.fromRGBA((r*255).toInt, (g*255).toInt, (b*255).toInt, (a*255).toInt).int
  }
}
