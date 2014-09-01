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

import geotrellis.proj4.datum._

class BasicCoordinateTransform(
  val sourceCRS: CoordinateReferenceSystem,
  val targetCRS: CoordinateReferenceSystem) extends CoordinateTransform {

  val doDatumTransform =
    sourceCRS.datum != targetCRS.datum

  private lazy val sourceGeoConv =
    GeocentricConverter(sourceCRS.datum.ellipsoid)

  private lazy val targetGeoConv =
    GeocentricConverter(targetCRS.datum.ellipsoid)

  // TODO: Remove
  private def dummy = ProjCoordinate(0,0)

  def transform(source: ProjCoordinate): ProjCoordinate = {
    val geoCoord: ProjCoordinate = {
      val p = sourceCRS.projection.inverseProjectRadians(source, dummy)
      if (doDatumTransform) datumTransform(p)
      else p
    }

    targetCRS.projection.projectRadians(geoCoord, dummy)
  }

  private def datumTransform(pt: ProjCoordinate) = {
    val p1 = sourceGeoConv.convertGeodeticToGeocentric(pt)
    val p2 = sourceCRS.datum.transformFromGeocentricToWgs84(p1)
    val p3 = targetCRS.datum.transformFromGeocentricToWgs84(p2)
    targetGeoConv.convertGeocentricToGeodetic(p3)
  }
}
