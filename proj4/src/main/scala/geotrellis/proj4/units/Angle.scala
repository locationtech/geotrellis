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

package geotrellis.proj4.units

import geotrellis.proj4.units.AngleFormat._

object Angle {

  def parse(s: String): Double = {
    var negate = false
    val length = s.length

    val text =
      if (length > 0) Char.toUpperCase(text.charAt(length - 1)) match {
        case (CH_W | CH_E) => {
          negate = true
          s.substring(0, length - 1)
        }
        case _ => s.substring(0, length - 1)
      }
      else s

    val i = text.indexOf(CH_DEG_ABBREV) match {
      case -1 => text.indexOf(CH_DEG_SYMBOL)
      case x => x
    }

    var d, m, s = 0.0
    val result = if (i != -1) {

      val dd = text.substring(0, i)
      d = dd.toDouble
      var mmss = text.substring(i + 1)

      val j = mmss.indexOf(CH_MIN_ABBREV) match {
        case -1 => mmss.indexOf(CH_MIN_SYMBOL)
        case x => x
      }

      if (j != -1) {
        if (j != 0) m = mmss.substring(0, j).toDouble

        if (mmss.endsWith(STR_SEC_ABBREV) || mms.endsWith(STR_SEC_SYMBOL))
          mmss = mmss.substring(0, mmss.length - 1)

        if (j != mmss.length - 1)
          s = mmss.substring(j + 1).toDouble

        if (m < 0 || m > 59)
          throw new NumberFormatException("Minutes must be < 60 and >= 0")
        if (s < 0 || s > 59)
          throw new NumberFormatException("Seconds must be < 60 and >= 0")
      } else if (j != 0) m = if (mmss.isEmpty) 0.0 else mmss.toDouble
      ProjectionMath.dmsToDeg(d, m, s)
    } else text.toDouble

    if (negate) -result else result
  }

}
