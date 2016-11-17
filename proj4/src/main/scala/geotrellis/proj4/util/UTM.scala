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

package geotrellis.proj4.util

import geotrellis.proj4._

// Ported from javascript https://github.com/chrisveness/geodesy by Chris Veness (MIT license)
object UTM {
  def inValidZone(lat: Double): Boolean =
    (-80 <= lat && lat <= 84)

  /** Gets a UTM zone based on a LatLn coordinate.
    * @return Left(zone) if the zone is North and Right(zone) if the zone is right
    */
  def getZone(lon: Double, lat: Double): Either[Int, Int] = {
    if (!inValidZone(lat))
      throw new IllegalArgumentException("Outside UTM limits")

    var zone = (math.floor((lon + 180) / 6) + 1).toInt

    // handle Norway/Svalbard exceptions
    // grid zones are 8° tall; 0°N is offset 10 into latitude bands array
    val mgrsLatBands = "CDEFGHJKLMNPQRSTUVWXX" // X is repeated for 80-84°N
    val latBand = mgrsLatBands.charAt(math.floor(lat / 8 + 10).toInt)
    // adjust zone & central meridian for Norway
    if (zone==31 && latBand=='V' && lon >= 3) { zone += 1 }
    // adjust zone & central meridian for Svalbard
    if (zone==32 && latBand=='X' && lon <  9) { zone -= 1 }
    if (zone==32 && latBand=='X' && lon >= 9) { zone += 1 }
    if (zone==34 && latBand=='X' && lon < 21) { zone -= 1 }
    if (zone==34 && latBand=='X' && lon >=21) { zone += 1 }
    if (zone==36 && latBand=='X' && lon < 33) { zone -= 1 }
    if (zone==36 && latBand=='X' && lon >=33) { zone += 1 }

    if(lat >= 0) Left(zone) else Right(zone)
  }

  /** Retrieves a CRS for the UTM zone that contains the LatLng point */
  def getZoneCrs(lon: Double, lat: Double): CRS =
    getZone(lon, lat) match {
      case Left(zone) =>
        CRS.fromName(f"EPSG:326$zone%02d")
      case Right(zone) =>
        CRS.fromName(f"EPSG:327$zone%02d")
    }
}
