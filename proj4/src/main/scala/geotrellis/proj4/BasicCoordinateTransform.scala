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

  val doDatumTransform = sourceCRS.datum != targetCRS.datum

  val transformViaGeocentric =
    doDatumTransform && ((sourceCRS.datum.getEllipsoid != targetCRS.datum.getEllipsoid) ||
      sourceCRS.datum.hasTransformToWGS84 || targetCRS.datum.hasTransformToWGS84)

  val sourceGeoConverter =
    if (transformViaGeocentric)
      Some(new GeocentricConverter(sourceCRS.datum.getEllipsoid))
    else
      None

  val targetGeoConverter =
    if (transformViaGeocentric)
      Some(new GeocentricConverter(targetCRS.datum.getEllipsoid))
    else
      None

  def transform(
    source: ProjCoordinate,
    target: ProjCoordinate): ProjCoordinate = {
    var (s, t, g) = (source, target, ProjCoordinate(0, 0))
    g = sourceCRS.projection.inverseProjectRadians(s, g)

    g = ProjCoordinate(g.x, g.y)

    g = if (doDatumTransform) datumTransform(g) else g

    targetCRS.projection.projectRadians(g, target)
  }

  private def datumTransformation(pc: ProjCoordinate): ProjCoordinate =
    if (doDatumTransform && transformViaGeocentric) {
      var p = sourceGeoConverter.convertGeodeticToGeocentric(pc)

      p =
        if (sourceCRS.datum.hasTransformToWGS84)
          sourceCRS.datum.transformFromGeocentricToWgs84(p)
        else
          p

      p =
        if (targetCRS.datum.hasTransformToWGS84)
          targetCRS.datum.transformFromGeocentricToWgs84(p)
        else p

      targetGeoConverter.convertGeocentricToGeodetic(p)
    } else pc

}
