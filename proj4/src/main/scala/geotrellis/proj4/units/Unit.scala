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

import java.text.ParseException
import java.text.NumberFormat

object Unit {

  val ANGLE_UNIT = 0
  val LENGTH_UNIT = 1
  val AREA_UNIT = 2
  val VOLUME_UNIT = 3

  val numberFormat = {
    val f = NumberFormat.getNumberInstance
    f.setMaximumFractionDigits(2)
    f.setGroupingUsed(false)
    f
  }

}

case class Unit(name: String, plural: String, abbreviation: String, value: Double) {

  import Unit.numberFormat

  def toBase(n: Double): Double = n * value

  def fromBase(n: Double): Double = n / value

  def parse(s: String): Double = try {
    numberFormat.parse(s).doubleValue
  } catch {
    case pe: ParseException => throw new NumberFormatException(pe.getMessage)
  }

  def format(n: Double): String = s"${numberFormat.format(n)} $abbreviation"

  def format(n: Double, abbrev: Boolean): String =
    if (abbrev) format(n) else numberFormat.format(n)

  def format(x: Double, y: Double, abbrev: Boolean): String =
    s"${numberFormat.format(x)}/${numberFormat.format(y)} " + (if (abbrev) abbreviation else "")

  def format(x: Double, y: Double): String = format(x, y, true)

  override def toString: String = plural

  override def equals(that: Any): Boolean = 
    that match {
      case unit: Unit => unit.value == value
      case _ => false
    }

}
