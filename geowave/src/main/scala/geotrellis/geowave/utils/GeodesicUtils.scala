/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.utils

import geotrellis.vector.Point
import squants.space._

object GeodesicUtils {
  def pointFromOriginDistanceBearing(originLatLng: Point, distance: Length, bearingAngle: Angle): Point = {
    val earthRadius = 6371000 // meters

    val bearingRadians = bearingAngle.toRadians
    val latRadians = math.toRadians(originLatLng.getY)
    val lngRadians = math.toRadians(originLatLng.getX)
    val dMeters = distance.toMeters

    val latRadians2 =
      math.asin(math.sin(latRadians) * math.cos(dMeters/earthRadius) + math.cos(latRadians)*math.sin(dMeters/earthRadius)*math.cos(bearingRadians))
    val lngRadians2 =
      lngRadians + math.atan2(math.sin(bearingRadians) * math.sin(dMeters/earthRadius) * math.cos(latRadians),
                       math.cos(dMeters/earthRadius) - math.sin(latRadians) * math.sin(latRadians))

    Point(math.toDegrees(lngRadians2), math.toDegrees(latRadians2))
  }
}