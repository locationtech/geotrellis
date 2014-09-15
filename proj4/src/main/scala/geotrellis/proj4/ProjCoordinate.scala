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

package geotrellis.proj4

import java.text.DecimalFormat

object ProjCoordinate {

  val DECIMAL_FORMAT_PATTERN = "0.0###############"
  val DECIMAL_FORMAT = new DecimalFormat(DECIMAL_FORMAT_PATTERN)

  def apply(args: String): ProjCoordinate = if (args.startsWith("ProjCoordinate: ")) {
    val parts = args.substring(17, args.length - 2).split(" ")

    if (parts.length != 2 && parts.length != 3)
      throw new IllegalArgumentException(
        "The input string was not in the proper format."
      )

    val x = parts(0).toDouble
    val y = parts(1).toDouble

    val z = if (parts.length == 3) parts(2).toDouble else Double.NaN

    ProjCoordinate(x, y, z)
  } else throw new IllegalArgumentException(
    "The input string was not in the proper format."
  )

}

case class ProjCoordinate(x: Double = 0.0, y: Double = 0.0, z: Double = Double.NaN) {

  import ProjCoordinate._

  def areXOrdinatesEqual(compare: ProjCoordinate, epsilon: Double): Boolean =
    math.abs(x - compare.x) < epsilon

  def areYOrdinatesEqual(compare: ProjCoordinate, epsilon: Double): Boolean =
    math.abs(y - compare.y) < epsilon

  def areZOrdinatesEqual(
    compare: ProjCoordinate,
    epsilon: Double): Boolean = (z, compare.z) match {
    case (Double.NaN, Double.NaN) => true
    case (Double.NaN, _) => false
    case (_, Double.NaN) => false
    case (a, b) => math.abs(a - b) < epsilon
  }

  // TODO: Big doubt this is ever used since in the Proj4j code
  // there was an enourmus bug parsing this in the constructor.
  override def toString: String =
    s"ProjCoordinate[$x $y $z]"

  lazy val toShortString: String = {
    val sb = new StringBuilder()
    builder.append("[")
    builder.append(DECIMAL_FORMAT.format(x).replace(",", "."))
    builder.append(", ")
    builder.append(DECIMAL_FORMAT.format(y).replace(",", "."))

    if (!Double.isNaN(z)) builder.append(s", $z")

    builder.append("]")

    builder.toString()
  }

  lazy val hasValidZOrdinate: Boolean = Double.isNaN(z)

  lazy val hasValidXAndYOrdinates: Boolean =
    !Double.isNaN(x) && !Double.isNaN(y) && !Double.isInfinite(x) && !Double.isInfinite(y)

}
