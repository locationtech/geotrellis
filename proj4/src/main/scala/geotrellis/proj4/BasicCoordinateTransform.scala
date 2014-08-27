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

  val doInverseProjection =
    (sourceCRS != null && sourceCRS != CoordinateReferenceSystem.CS_GEO)
  val doForwardProjection =
    (targetCRS != null && targetCRS != CoordinateReferenceSystem.CS_GEO)
  val doDatumTransform = doInverseProjection &&
  doForwardProjection && sourceCRS.getDatum != targetCRS.getDatum

  val transformViaGeocentric =
    doDatumTransform && ((sourceCRS.getDatum.getEllipsoid != targetCRS.getDatum.getEllipsoid) ||
      sourceCRS.getDatum.hasTransformToWGS84 || targetCRS.getDatum.hasTransformToWGS84)

  val sourceGeoConv =
    if (transformViaGeocentric)
      Some(new GeocentricConverter(sourceCRS.getDatum.getEllipsoid))
    else
      None

  val targetGeoConv =
    if (transformViaGeocentric)
      Some(new GeocentricConverter(targetCRS.getDatum.getEllipsoid))
    else
      None

  def transform(
    source: ProjCoordinate,
    target: ProjCoordinate): ProjCoordinate = {
    if (doInverseProjection)
      sourceCRS.getProjection.inverseProjectRadians(source, geoCoord)
    else
      geoCoord.setValue(source)

    geoCoord.clearZ

    if (doDatumTransform) datumTransform(geoCoord)

    if (doForwardProjection)
      targetCRS.getProjection().projectRadians(geoCoord, target)
    else
      target.setValue(geoCoord)

    target
  }

  private def datumTransformation(pt: ProjCoordinate) =
    if (sourceCRS.getDatum != targetCRS.getDatum && transformViaGeocentric) {
      sourceGeoConv.convertGeodeticToGeocentric(pt)

      if (sourceCRS.getDatum.hasTransformToWGS84)
        sourceCRS.getDatum.transformFromGeocentricToWgs84(pt)

      if (targetCRS.getDatum.hasTransformToWGS84)
        targetCRS.getDatum.transformFromGeocentricToWgs84(pt)

      targettGeoConv.convertGeocentricToGeodetic(pt)
    }

}
