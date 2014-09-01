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

package geotrellis.proj4.parser

object Proj4Keyword {

  val a = "a"
  val b = "b"
  val f = "f"
  val alpha = "alpha"
  val datum = "datum"
  val ellps = "ellps"
  val es = "es"

  val azi = "azi"
  val k = "k"
  val k_0 = "k_0"
  val lat_ts = "lat_ts"
  val lat_0 = "lat_0"
  val lat_1 = "lat_1"
  val lat_2 = "lat_2"
  val lon_0 = "lon_0"
  val lonc = "lonc"
  val pm = "pm"

  val proj = "proj"

  val R = "R"
  val R_A = "R_A"
  val R_a = "R_a"
  val R_V = "R_V"
  val R_g = "R_g"
  val R_h = "R_h"
  val R_lat_a = "R_lat_a"
  val R_lat_g = "R_lat_g"
  val rf = "rf"

  val south = "south"
  val to_meter = "to_meter"
  val towgs84 = "towgs84"
  val units = "units"
  val x_0 = "x_0"
  val y_0 = "y_0"
  val zone = "zone"

  val title = "title"
  val nadgrids = "nadgrids"
  val no_defs = "no_defs"
  val wktext = "wktext"

  private val supportedParams = 
    Set(
      a, rf, f, alpha, es, b, datum, ellps,
      R_A,
      k, k_0, lat_ts, lat_0, lat_1, lat_2, lon_0, lonc,
      x_0, y_0,
      proj, south, towgs84, to_meter, units, zone,
      title, no_defs, wktext, nadgrids
    )

  def isSupported(paramKey: String): Boolean =
    supportedParams.contains(paramKey)

  /** Returns list of unsupported keys if is not valid, otherwise None */
  def invalidKeys(keySet: Set[String]): Option[List[String]] = {
    val l = keySet.filter(!isSupported(_)).toList
    if(l.size != 0)
      Some(l)
    else
      None
  }
}
