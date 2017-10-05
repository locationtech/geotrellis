/*
 * Copyright 2017 Azavea
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

package geotrellis.util

object Haversine {
  val EARTH_RADIUS = 6378137d // Use what gdal2tiles uses.

  // (x: Double, y: Double) points
  def apply(start: (Double, Double), end: (Double, Double), R: Double = EARTH_RADIUS): Double = {
    val dLat = math.toRadians(end._2 - start._2)
    val dLon = math.toRadians(end._1 - start._1)
    val lat1 = math.toRadians(start._2)
    val lat2 = math.toRadians(end._2)

    val a =
      math.sin(dLat / 2) * math.sin(dLat / 2) +
      math.sin(dLon / 2) * math.sin(dLon / 2) * math.cos(lat1) * math.cos(lat2)
    val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    R * c
  }
}
