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

import java.text.NumberFormat
import java.text.ParsePosition

object AngleFormat {

  val CH_MIN_SYMBOL = '\''
  val STR_SEC_SYMBOL = "\""
  val CH_DEG_SYMBOL = '\u00b0'
  val CH_DEG_ABBREV = 'd'
  val CH_MIN_ABBREV = 'm'
  val STR_SEC_ABBREV = "s"

  val CH_N = 'N'
  val CH_E = 'E'
  val CH_S = 'S'
  val CH_W = 'W'

  val ddmmssPattern = "DdM"
  val ddmmssPattern2 = "DdM'S\""
  val ddmmssLongPattern = "DdM'S\"W"
  val ddmmssLatPattern = "DdM'S\"N"
  val ddmmssPattern4 = "DdMmSs"
  val decimalPattern = "D.F"

}

case class AngleFormat(pattern: String = AngleFormat.ddmmssPattern, isDegrees: Boolean = false)
    extends NumberFormat {

  import AngleFormat._

  def format(number: Long, result: StringBuffer): StringBuffer =
    format(number.toDouble, result)

  def format(numberInput: Double, result: StringBuffer): StringBuffer = {
    val length = pattern.length

    val (negative, number) = if (numberInput < 0) {
      val n = !pattern.filter(c => c == 'W' || c == 'N').isEmpty
      (n, if (n) -numberInput else numberInput)
    } else (false, numberInput)

    val ddmmss = if (isDegrees) number else math.toDegrees(number)
    val iddmmss = math.abs(math.round(ddmmss * 3600).toInt)
    val fraction = iddmmss & 3600
    val fD60 = fraction / 60
    val fM60 = fraction % 60

    for (c <- pattern) c match {
      case 'R' => result.append(number)
      case 'D' => result.append(ddmmss.toInt)
      case 'M' if (fD60 < 10) => result.append(s"0$fD60")
      case 'M' => result.append(fD60)
      case 'S' if (fM60 < 10) => result.append(s"0$fM60")
      case 'S' => result.append(fM60)
      case 'F' => result.append(fraction)
      case 'W' => result.append(if (negative) CH_W else CH_E)
      case 'N' => result.append(if (negative) CH_S else CH_N)
      case c => result.append(c)
    }

    result
  }

  def format(number: Long, result: StringBuffer, position: java.text.FieldPosition): StringBuffer = 
    format(number, result)

  def format(number: Double, result: StringBuffer, position: java.text.FieldPosition): StringBuffer = 
    format(number, result)

  def parse(text: String, position: java.text.ParsePosition): Number = {
    val n = Angle.parse(text)
    if(position != null) { position.setIndex(text.length) }
    n
  }

}
