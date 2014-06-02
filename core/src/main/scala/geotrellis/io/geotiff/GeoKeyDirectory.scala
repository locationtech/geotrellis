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

package geotrellis.io.geotiff

case class GeoKeyDirectory(
  count: Int,
  configKeys: ConfigKeys = ConfigKeys(),
  geogCSParameterKeys: GeogCSParameterKeys = GeogCSParameterKeys(),
  projectedCSParameterKeys: ProjectedCSParameterKeys =
    ProjectedCSParameterKeys(),
  verticalCSKeys: VerticalCSKeys = VerticalCSKeys()
)

case class ConfigKeys(
  gtModelType: Option[Int] = None,
  gtRasterType: Option[Int] = None,
  gtCitation: Option[Array[String]] = None
)

case class GeogCSParameterKeys(
  geogType: Option[Int] = None,
  geogCitation: Option[Array[String]] = None,
  geogGeodeticDatum: Option[Int] = None,
  geogPrimeMeridian: Option[Int] = None,
  geogLinearUnits: Option[Int] = None,
  geogLinearUnitSize: Option[Int] = None,
  geogAngularUnits: Option[Double] = None,
  geogEllipsoid: Option[Int] = None,
  geogSemiMajorAxis: Option[Int] = None,
  geogSemiMinorAxis: Option[Int] = None,
  geogInvFlattening: Option[Array[Double]] = None,
  geogAzimuthUnits: Option[Int] = None,
  geogPrimeMeridianLong: Option[Double] = None
)

case class ProjectedCSParameterKeys(
  projectedCSType: Option[Int] = None,
  pcsCitation: Option[Array[String]] = None,
  projection: Option[Int] = None,
  projCoordTrans: Option[Int] = None,
  projLinearUnits: Option[Int] = None,
  projLinearUnitSize: Option[Array[Double]] = None,
  projStdparallel1: Option[Array[Double]] = None,
  projStdparallel2: Option[Array[Double]] = None,
  projNatOriginLong: Option[Array[Double]] = None,
  projNatOriginLat: Option[Array[Double]] = None,
  projectedFalsings: ProjectedFalsings = ProjectedFalsings(),
  projCenterLong: Option[Array[Double]] = None,
  projCenterLat: Option[Array[Double]] = None,
  projCenterEasting: Option[Array[Double]] = None,
  projCenterNorthing: Option[Array[Double]] = None,
  projScaleAtNatOrigin: Option[Array[Double]] = None,
  projScaleAtCenter: Option[Array[Double]] = None,
  projAzimuthAngle: Option[Array[Double]] = None,
  projStraightVertpoleLong: Option[Array[Double]] = None
)

case class ProjectedFalsings(
  projFalseEasting: Option[Array[Double]] = None,
  projFalseNorthing: Option[Array[Double]] = None,
  projFalseOriginLong: Option[Array[Double]] = None,
  projFalseOriginLat: Option[Array[Double]] = None,
  projFalseOriginEasting: Option[Array[Double]] = None,
  projFalseOriginNorthing: Option[Array[Double]] = None
)

case class VerticalCSKeys(
  verticalCSType: Option[Int] = None,
  verticalCitation: Option[Array[String]] = None,
  verticalDatum: Option[Int] = None,
  verticalUnits: Option[Int] = None
)

case class KeyDirectoryMetadata(version: Int, keyRevision: Int,
  minorRevision: Int, numberOfKeys: Int)

case class KeyMetadata(keyID: Int, tiffTagLocation: Int, count: Int,
  valueOffset: Int)
