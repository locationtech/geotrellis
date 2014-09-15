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

object Units {

  val DEGREES = Unit("degree", "degrees", "deg", 1)
  val RADIANS = Unit("radian", "radians", "rad", math.toDegrees(1))
  val ARC_MINUTES = Unit("arc minute", "arc minutes", "min", 1/60.0)
  val ARC_SECONDS = Unit("arc second", "arc seconds", "sec", 1/3600.0)

  val KILOMETRES = Unit("kilometre", "kilometres", "km", 1000)
  val METRES = Unit("metre", "metres", "m", 1)
  val DECIMETRES = Unit("decimetre", "decimetres", "dm", 0.1)
  val CENTIMETRES = Unit("centimetre", "centimetres", "cm", 0.01)
  val MILLIMETRES = Unit("millimetre", "millimetres", "mm", 0.001)

  val NAUTICAL_MILES = Unit("nautical mile", "nautical miles", "kmi", 1852)
  val MILES = Unit("mile", "miles", "mi", 1609.344)
  val CHAINS = Unit("chain", "chains", "ch", 20.1168)
  val YARDS = Unit("yard", "yards", "yd", 0.9144)
  val FEET = Unit("foot", "feet", "ft", 0.3048)
  val INCHES = Unit("inch", "inches", "in", 0.0254)

  val US_MILES = Unit("U.S. mile", "U.S. miles", "us-mi", 1609.347218694437)
  val US_CHAINS = Unit("U.S. chain", "U.S. chains", "us-ch", 20.11684023368047)
  val US_YARDS = Unit("U.S. yard", "U.S. yards", "us-yd", 0.914401828803658)
  val US_FEET = Unit("U.S. foot", "U.S. feet", "us-ft", 0.304800609601219)
  val US_INCHES = Unit("U.S. inch", "U.S. inches", "us-in", 1.0/39.37)

  val FATHOMS = Unit("fathom", "fathoms", "fath", 1.8288)
  val LINKS = Unit("link", "links", "link", 0.201168)

  val POINTS = Unit("point", "points", "point", 0.0254/72.27)

  val units = Array(
    DEGREES,
    KILOMETRES, METRES, DECIMETRES, CENTIMETRES, MILLIMETRES,
    MILES, YARDS, FEET, INCHES,
    US_MILES, US_YARDS, US_FEET, US_INCHES,
    NAUTICAL_MILES
  )

  def findUnits(name: String): Unit =
    units.filter(u => u.name == name || u.plural == name || u.abbreviation == name)
      .headOption getOrElse METRES

  def convert(v: Double, from: Unit, to: Unit): Double =
    if (from == to) v
    else to.fromBase(from.toBase(v))

}
