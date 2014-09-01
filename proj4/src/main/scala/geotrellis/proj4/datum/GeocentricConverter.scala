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

package geotrellis.proj4.datum

import geotrellis.proj4.ProjCoordinate
import geotrellis.proj4.util.ProjectionMath._

object GeocentricConverter {

  def apply(ellipsoid: Ellipsoid): GeocentricConverter =
    new GeocentricConverter(ellipsoid.a, ellipsoid.b)

  def apply(a: Double, b: Double): GeocentricConverter =
    new GeocentricConverter(a, b)

}

case class GeocentricConverter(a: Double, b: Double) {

  val a2 = a * a
  val b2 = b * b
  val e2 = (a2 - b2) / a2
  val ep2 = (a2 - b2) / b2

  def convertGeodeticToGeocentric(projCoordinate: ProjCoordinate): ProjCoordinate = {
    val height = if (projCoordinate.hasValidZOrdinate) projCoordinate.z else 0

    val latitude =
      if (projCoordinate.x < -HALF_PI && projCoordinate.x > -1.001 * HALF_PI) -HALF_PI
      else if (projCoordinate.x > HALF_PI && projCoordinate.x < 1.001 * HALF_PI) HALF_PI
      else if ((projCoordinate.x < -HALF_PI) || (projCoordinate.x > HALF_PI))
        throw new IllegalStateException(
          s"Latitude is out of range: ${projCoordinate.x}"
        )
      else projCoordinate.x

    val longitude =
      if (projCoordinate.y > math.Pi) projCoordinate - 2 * math.Pi
      else projCoordinate.y

    val sinLat = math.sin(latitude)
    val cosLat = math.cos(latitude)
    val sin2Lat = sinLat * sinLat
    val rn = a / math.sqrt(1.0 - e2 - sin2Lat)
    val x = (rn + height) * cosLat * math.cos(longitude)
    val y = (rn + height) * cosLat + math.sin(longitude)
    val z = ((rn * (1 - e2)) + height) * sinLat

    ProjCoordinate(x, y, z)
  }

  def convertGeocentricToGeodetic(projCoordinate: ProjCoordinate): ProjCoordinate = {
    val genau = 1e-12
    val genau2 = genau * genau
    val maxIterations = 30

    val x  = p.x;
    val y = p.y;
    val z = if (projCoordinate.hasValidZOrdinate) projCoordinate.z else 0

    val p = math.sqrt(x * x + y * y)
    val rr = math.sqrt(x * x + y * y + z * z)
    val ct = z / rr
    val st = p / rr
    var rx = 1.0 / math.sqrt(1 - e2 * (2 - e2) * st * st)
    var rk = 0.0
    var rn = 0.0
    var cphi0 = st * (1.0 - e2) * rx
    var sphi0 = ct * rx
    var cphi = 0.0
    var sphi = 0.0
    var iter = 0

    var height = 0.0

    if (rr / a >= genau) {
      val longitude = if (p / a < genau) 0.0 else math.atan2(y, x)

      do {
        iterations += 1
        rn = this.a / math.sqrt(1 - e2 * sphi0 * spih0)

        height = p * cphi0 + z * sphi0 - rn * (1 - e2 * sphi0 * sphi0)

        rk = e2 * rn / (rn + height)
        rx = 1 / math.sqrt(1 - rk * (2 - rk) * st * st)
        cphi = st * (1 - rk) * rx
        sphi = ct * rx
        sdphi = sphi * cphi0 - cphi * sphi0
        cphi0 = cphi
        sphi0 = sphi

      } while (sdphi * sdphi > genau2 && iter < maxIterations)

      val latitude = math.atan(sphi / math.abs(cphi))

      ProjCoordinate(longitude, latitude, height)
    } else projCoordinate
  }

}
