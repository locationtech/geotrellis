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

package geotrellis.raster.io.geotiff.reader

import monocle.syntax._
import monocle.Macro._

import scala.collection.immutable.HashMap

object GeoKeys {

  val GTModelTypeGeoKey = 1024
  val GTRasterTypeGeoKey = 1025
  val GTCitationGeoKey = 1026

  val GeogTypeGeoKey = 2048
  val GeogCitationGeoKey = 2049
  val GeogGeodeticDatumGeoKey = 2050
  val GeogPrimeMeridianGeoKey = 2051
  val GeogLinearUnitsGeoKey = 2052
  val GeogLinearUnitSizeGeoKey = 2053
  val GeogAngularUnitsGeoKey = 2054
  val GeogAngularUnitSizeGeoKey = 2055
  val GeogEllipsoidGeoKey = 2056
  val GeogSemiMajorAxisGeoKey = 2057
  val GeogSemiMinorAxisGeoKey = 2058
  val GeogInvFlatteningGeoKey = 2059
  val GeogAzimuthUnitsGeoKey = 2060
  val GeogPrimeMeridianLongGeoKey = 2061

  val ProjectedCSTypeGeoKey = 3072
  val PCSCitationGeoKey = 3073
  val ProjectionGeoKey = 3074
  val ProjCoordTransGeoKey = 3075
  val ProjLinearUnitsGeoKey = 3076
  val ProjLinearUnitSizeGeoKey = 3077
  val ProjStdParallel1GeoKey = 3078
  val ProjStdParallel2GeoKey = 3079
  val ProjNatOriginLongGeoKey = 3080
  val ProjNatOriginLatGeoKey = 3081
  val ProjFalseEastingGeoKey = 3082
  val ProjFalseNorthingGeoKey = 3083
  val ProjFalseOriginLongGeoKey = 3084
  val ProjFalseOriginLatGeoKey = 3085
  val ProjFalseOriginEastingGeoKey = 3086
  val ProjFalseOriginNorthingGeoKey = 3087
  val ProjCenterLongGeoKey = 3088
  val ProjCenterLatGeoKey = 3089
  val ProjCenterEastingGeoKey = 3090
  val ProjCenterNorthingGeoKey = 3091
  val ProjScaleAtNatOriginGeoKey = 3092
  val ProjScaleAtCenterGeoKey = 3093
  val ProjAzimuthAngleGeoKey = 3094
  val ProjStraightVertPoleLongGeoKey = 3095

  val VerticalCSTypeGeoKey = 4096
  val VerticalCitationGeoKey = 4097
  val VerticalDatumGeoKey = 4098
  val VerticalUnitsGeoKey = 4099

}

object CommonPublicValues {

  val UndefinedCPV = 0
  val UserDefinedCPV = 32767
}

case class ConfigKeys(
  gtModelType: Option[Int] = None,
  gtRasterType: Option[Int] = None,
  gtCitation: Option[Vector[String]] = None
)

case class GeogCSParameterKeys(
  geogType: Option[Int] = None,
  geogCitation: Option[Vector[String]] = None,
  geogGeodeticDatum: Option[Int] = None,
  geogPrimeMeridian: Option[Int] = None,
  geogLinearUnits: Option[Int] = None,
  geogLinearUnitSize: Option[Vector[Double]] = None,
  geogAngularUnits: Option[Int] = None,
  geogAngularUnitSize: Option[Vector[Double]] = None,
  geogEllipsoid: Option[Int] = None,
  geogSemiMajorAxis: Option[Vector[Double]] = None,
  geogSemiMinorAxis: Option[Vector[Double]] = None,
  geogInvFlattening: Option[Vector[Double]] = None,
  geogAzimuthUnits: Option[Int] = None,
  geogPrimeMeridianLong: Option[Vector[Double]] = None
)

case class ProjectedCSParameterKeys(
  projectedCSType: Option[Int] = None,
  pcsCitation: Option[Vector[String]] = None,
  projection: Option[Int] = None,
  projCoordTrans: Option[Int] = None,
  projLinearUnits: Option[Int] = None,
  projLinearUnitSize: Option[Vector[Double]] = None,
  projStdParallel1: Option[Vector[Double]] = None,
  projStdParallel2: Option[Vector[Double]] = None,
  projNatOriginLong: Option[Vector[Double]] = None,
  projNatOriginLat: Option[Vector[Double]] = None,
  projectedFalsings: ProjectedFalsings = ProjectedFalsings(),
  projCenterLong: Option[Vector[Double]] = None,
  projCenterLat: Option[Vector[Double]] = None,
  projCenterEasting: Option[Vector[Double]] = None,
  projCenterNorthing: Option[Vector[Double]] = None,
  projScaleAtNatOrigin: Option[Vector[Double]] = None,
  projScaleAtCenter: Option[Vector[Double]] = None,
  projAzimuthAngle: Option[Vector[Double]] = None,
  projStraightVertPoleLong: Option[Vector[Double]] = None
)

case class ProjectedFalsings(
  projFalseEasting: Option[Vector[Double]] = None,
  projFalseNorthing: Option[Vector[Double]] = None,
  projFalseOriginLong: Option[Vector[Double]] = None,
  projFalseOriginLat: Option[Vector[Double]] = None,
  projFalseOriginEasting: Option[Vector[Double]] = None,
  projFalseOriginNorthing: Option[Vector[Double]] = None
)

case class VerticalCSKeys(
  verticalCSType: Option[Int] = None,
  verticalCitation: Option[Vector[String]] = None,
  verticalDatum: Option[Int] = None,
  verticalUnits: Option[Int] = None
)

case class NonStandardizedKeys(
  shortMap: HashMap[Int, Int] = HashMap[Int, Int](),
  doublesMap: HashMap[Int, Vector[Double]] = HashMap[Int, Vector[Double]](),
  asciisMap: HashMap[Int, Vector[String]] = HashMap[Int, Vector[String]]()
)

case class GeoKeyDirectoryMetadata(version: Int, keyRevision: Int,
  minorRevision: Int, numberOfKeys: Int)

case class GeoKeyMetadata(keyID: Int, tiffTagLocation: Int, count: Int,
  valueOffset: Int)

case class GeoKeyDirectory(
  count: Int,
  configKeys: ConfigKeys = ConfigKeys(),
  geogCSParameterKeys: GeogCSParameterKeys = GeogCSParameterKeys(),
  projectedCSParameterKeys: ProjectedCSParameterKeys =
    ProjectedCSParameterKeys(),
  verticalCSKeys: VerticalCSKeys = VerticalCSKeys(),
  nonStandardizedKeys: NonStandardizedKeys = NonStandardizedKeys()
) {

  def getDoublesVectorFromMaps(key: Int) = {
    val doublesMap = nonStandardizedKeys.doublesMap

    if (!doublesMap.contains(key)) {
      val shortMap = nonStandardizedKeys.shortMap
      if (!shortMap.contains(key)) throw new MalformedGeoTiffException(
        s"couldn't find geokey $key"
      )
      else Vector(shortMap(key).toDouble)
    } else doublesMap(key)
  }

  def getDoubleFromMaps(key: Int) = getDoublesVectorFromMaps(key)(0)

}

object GeoKeyDirectoryLenses {

  val countLens = mkLens[GeoKeyDirectory, Int]("count")

  val configKeysLens = mkLens[GeoKeyDirectory, ConfigKeys]("configKeys")

  val gtModelTypeLens = configKeysLens |-> mkLens[ConfigKeys,
    Option[Int]]("gtModelType")
  val gtRasterTypeLens = configKeysLens |-> mkLens[ConfigKeys,
    Option[Int]]("gtRasterType")
  val gtCitationLens = configKeysLens |-> mkLens[ConfigKeys,
    Option[Vector[String]]]("gtCitation")

  val geogCSParameterKeysLens = mkLens[GeoKeyDirectory,
    GeogCSParameterKeys]("geogCSParameterKeys")

  val geogTypeLens = geogCSParameterKeysLens |-> mkLens[GeogCSParameterKeys,
    Option[Int]]("geogType")
  val geogCitationLens = geogCSParameterKeysLens |-> mkLens[GeogCSParameterKeys,
    Option[Vector[String]]]("geogCitation")
  val geogGeodeticDatumLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogGeodeticDatum")
  val geogPrimeMeridianLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogPrimeMeridian")
  val geogLinearUnitsLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogLinearUnits")
  val geogLinearUnitSizeLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogLinearUnitSize")
  val geogAngularUnitsLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogAngularUnits")
  val geogAngularUnitSizeLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogAngularUnitSize")
  val geogEllipsoidLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogEllipsoid")
  val geogSemiMajorAxisLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogSemiMajorAxis")
  val geogSemiMinorAxisLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogSemiMinorAxis")
  val geogInvFlatteningLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogInvFlattening")
  val geogAzimuthUnitsLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Int]]("geogAzimuthUnits")
  val geogPrimeMeridianLongLens = geogCSParameterKeysLens |-> mkLens[
    GeogCSParameterKeys, Option[Vector[Double]]]("geogPrimeMeridianLong")

  val projectedCSParameterKeysLens = mkLens[GeoKeyDirectory,
    ProjectedCSParameterKeys]("projectedCSParameterKeys")

  val projectedCSTypeLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Int]]("projectedCSType")
  val pcsCitationLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[String]]]("pcsCitation")
  val projectionLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Int]]("projection")
  val projCoordTransLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Int]]("projCoordTrans")
  val projLinearUnitsLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Int]]("projLinearUnits")
  val projLinearUnitSizeLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projLinearUnitSize")
  val projStdParallel1Lens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projStdParallel1")
  val projStdParallel2Lens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projStdParallel2")
  val projNatOriginLongLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projNatOriginLong")
  val projNatOriginLatLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projNatOriginLat")

  val projectedFalsingsLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, ProjectedFalsings]("projectedFalsings")

  val projFalseEastingLens = projectedFalsingsLens |-> mkLens[ProjectedFalsings,
    Option[Vector[Double]]]("projFalseEasting")
  val projFalseNorthingLens = projectedFalsingsLens |-> mkLens[
    ProjectedFalsings, Option[Vector[Double]]]("projFalseNorthing")
  val projFalseOriginLongLens = projectedFalsingsLens |-> mkLens[
    ProjectedFalsings, Option[Vector[Double]]]("projFalseOriginLong")
  val projFalseOriginLatLens = projectedFalsingsLens |-> mkLens[
    ProjectedFalsings, Option[Vector[Double]]]("projFalseOriginLat")
  val projFalseOriginEastingLens = projectedFalsingsLens |-> mkLens[
    ProjectedFalsings, Option[Vector[Double]]]("projFalseOriginEasting")
  val projFalseOriginNorthingLens = projectedFalsingsLens |-> mkLens[
    ProjectedFalsings, Option[Vector[Double]]]("projFalseOriginNorthing")

  val projCenterLongLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projCenterLong")
  val projCenterLatLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projCenterLat")
  val projCenterEastingLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projCenterEasting")
  val projCenterNorthingLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projCenterNorthing")
  val projScaleAtNatOriginLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projScaleAtNatOrigin")
  val projScaleAtCenterLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projScaleAtCenter")
  val projAzimuthAngleLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys, Option[Vector[Double]]]("projAzimuthAngle")
  val projStraightVertPoleLongLens = projectedCSParameterKeysLens |-> mkLens[
    ProjectedCSParameterKeys,
    Option[Vector[Double]]]("projStraightVertPoleLong")

  val verticalCSKeysLens = mkLens[GeoKeyDirectory,
    VerticalCSKeys]("verticalCSKeys")

  val verticalCSTypeLens = verticalCSKeysLens |-> mkLens[VerticalCSKeys,
    Option[Int]]("verticalCSType")
  val verticalCitationLens = verticalCSKeysLens |-> mkLens[VerticalCSKeys,
    Option[Vector[String]]]("verticalCitation")
  val verticalDatumLens = verticalCSKeysLens |-> mkLens[VerticalCSKeys,
    Option[Int]]("verticalDatum")
  val verticalUnitsLens = verticalCSKeysLens |-> mkLens[VerticalCSKeys,
    Option[Int]]("verticalUnits")

  val nonStandardizedKeysLens = mkLens[GeoKeyDirectory,
    NonStandardizedKeys]("nonStandardizedKeys")

  val geoKeyShortMapLens = nonStandardizedKeysLens |-> mkLens[NonStandardizedKeys,
    HashMap[Int, Int]]("shortMap")
  val geoKeyDoublesMapLens = nonStandardizedKeysLens |-> mkLens[NonStandardizedKeys,
    HashMap[Int, Vector[Double]]]("doublesMap")
  val geoKeyAsciisMapLens = nonStandardizedKeysLens |-> mkLens[NonStandardizedKeys,
    HashMap[Int, Vector[String]]]("asciisMap")

}
