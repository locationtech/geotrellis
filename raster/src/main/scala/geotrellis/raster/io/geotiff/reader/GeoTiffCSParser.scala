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

import scala.collection.immutable.HashMap

import monocle.syntax._

import geotrellis.raster.io.geotiff.reader.GeoKeyDirectoryLenses._
import geotrellis.raster.io.geotiff.reader.CommonPublicValues._
import geotrellis.raster.io.geotiff.reader.GeoKeys._
import geotrellis.raster.io.geotiff.reader.ModelTypes._
import geotrellis.raster.io.geotiff.reader.MapSystems._
import geotrellis.raster.io.geotiff.reader.ProjectedLinearUnits._
import geotrellis.raster.io.geotiff.reader.GeographicCSTypes._
import geotrellis.raster.io.geotiff.reader.EPSGProjectionTypes._
import geotrellis.raster.io.geotiff.reader.GDALEPSGProjectionTypes._
import geotrellis.raster.io.geotiff.reader.ProjectionTypesMap._
import geotrellis.raster.io.geotiff.reader.PrimeMeridianTypes._
import geotrellis.raster.io.geotiff.reader.AngularUnitTypes._
import geotrellis.raster.io.geotiff.reader.DatumTypes._
import geotrellis.raster.io.geotiff.reader.EllipsoidTypes._
import geotrellis.raster.io.geotiff.reader.CoordinateTransformTypes._

import geotrellis.proj4.EPSGCSVReader
import geotrellis.proj4.CSVFileConstants._

case class GeoTiffGDALParameters(
  var defNSet: Int = 1,
  var model: Int = UserDefinedCPV,
  var pcs: Int = UserDefinedCPV,
  var gcs: Int = UserDefinedCPV,
  var length: Int = UserDefinedCPV,
  var lengthInMeters: Double = 1.0,
  var angle: Int = UserDefinedCPV,
  var angleInDegrees: Double = 1.0,
  var datum: Int = UserDefinedCPV,
  var ellipsoid: Int = UserDefinedCPV,
  var semiMajor: Double = 0.0,
  var semiMinor: Double = 0.0,
  var pm: Int = UserDefinedCPV,
  var pmLongToGreenwich: Double = 0.0,
  var projCode: Int = UserDefinedCPV,
  var projection: Int = UserDefinedCPV,
  var ctProjection: Int = UserDefinedCPV,
  var mapSystem: Int = UserDefinedCPV,
  var zone: Int = 0,
  var projectionParameters: Array[(Int, Double)] = Array()
)

object GeoTiffCSParser {

  def apply(directory: ImageDirectory) = new GeoTiffCSParser(directory)

}

/**
  * This class is indirectly ported from the GDAL github repository.
  */

class GeoTiffCSParser(directory: ImageDirectory) {

  private val geoKeyDirectory = directory.geoKeyDirectory

  private val csvReader = EPSGCSVReader()

  def getProj4String: Option[String] = getProj4String(createGeoTiffGDALParameters)

  private def createGeoTiffGDALParameters: GeoTiffGDALParameters = {
    val gtgp = GeoTiffGDALParameters()

    gtgp.model = geoKeyDirectory |-> gtModelTypeLens get match {
      case -1 => UserDefinedCPV
      case modelType => modelType
    }

    val linearUnits = (geoKeyDirectory |-> geogLinearUnitsLens get) getOrElse 9001

    gtgp.pcs = geoKeyDirectory |-> projectedCSTypeLens get match {
      case pcs if (pcs != -1 && pcs != UserDefinedCPV) => {
        getPCSData(pcs, gtgp)
        pcs
      }
      case _ => UserDefinedCPV
    }

    if (gtgp.pcs != UserDefinedCPV && gtgp.projCode == UserDefinedCPV) {
      val (optDatum, optZone, optMapSystem) = pcsToDatumZoneAndMapSystem(gtgp.pcs)

      gtgp.zone = optZone.getOrElse(gtgp.zone)


      if (!optMapSystem.isEmpty) {
        gtgp.projCode = mapSystemToProjection(optMapSystem.get, gtgp.zone)
        gtgp.gcs = optDatum.getOrElse(gtgp.gcs)
      }
    }

    if (gtgp.projCode == UserDefinedCPV)
      gtgp.projCode = (geoKeyDirectory |-> projectionLens get) getOrElse gtgp.projCode

    if (gtgp.projCode != UserDefinedCPV) {
      val (optProjection, optProjectionParameters) =
        getProjectionFromTransformationCode(gtgp.projCode)

      gtgp.ctProjection = optProjection match {
        case Some(projection) => epsgProjMethodToCTProjMethod(projection)
        case None => gtgp.ctProjection
      }

      gtgp.projectionParameters =
        if (gtgp.projectionParameters.size == 0) Array.ofDim[(Int, Double)](7)
        else gtgp.projectionParameters

      setGTParameterIdentities(gtgp.ctProjection, gtgp.projectionParameters)
    }

    gtgp.gcs = (geoKeyDirectory |-> geogTypeLens get) getOrElse gtgp.gcs

    if (gtgp.gcs != UserDefinedCPV) {
      val (optPm, optAngle, optDatum) = getGCSInfo(gtgp.gcs)
      gtgp.pm = optPm getOrElse gtgp.pm
      gtgp.angle = optAngle getOrElse gtgp.angle
      gtgp.datum = optDatum getOrElse gtgp.datum
    }

    gtgp.angle = (geoKeyDirectory |-> geogAngularUnitsLens get) getOrElse gtgp.angle

    if (gtgp.angle != UserDefinedCPV) {
      val optAngleInDegrees = getAngleInfo(gtgp.angle)
      gtgp.angleInDegrees = optAngleInDegrees getOrElse UserDefinedCPV
    }

    gtgp.datum = (geoKeyDirectory |-> geogGeodeticDatumLens get) getOrElse gtgp.datum

    if (gtgp.datum != UserDefinedCPV) {
      val optEllipsoid = getDatumInfo(gtgp.datum)
      gtgp.ellipsoid = optEllipsoid getOrElse gtgp.ellipsoid
    }

    gtgp.ellipsoid = (geoKeyDirectory |-> geogEllipsoidLens get) getOrElse gtgp.ellipsoid

    if (gtgp.ellipsoid != UserDefinedCPV) {
      val (optSemiMajor, optSemiMinor) = getEllipsoidInfo(gtgp.ellipsoid)

      gtgp.semiMajor = optSemiMajor getOrElse gtgp.semiMajor
      gtgp.semiMinor = optSemiMinor getOrElse gtgp.semiMinor
    }

    gtgp.semiMajor = (geoKeyDirectory |-> geogSemiMajorAxisLens get) getOrElse gtgp.semiMajor
    gtgp.semiMinor = (geoKeyDirectory |-> geogSemiMinorAxisLens get) getOrElse gtgp.semiMinor

    val optInvFlattning = geoKeyDirectory |-> geogInvFlatteningLens get

    if (!optInvFlattning.isEmpty) {
      val invFlattening = optInvFlattning.get

      gtgp.semiMinor =
        if (invFlattening != 0.0) semiMinorComp(gtgp.semiMajor, invFlattening)
        else gtgp.semiMajor
    }

    gtgp.pm = (geoKeyDirectory |-> geogPrimeMeridianLens get) getOrElse gtgp.pm

    if (gtgp.pm != UserDefinedCPV) {
      val optPMLongToGreenwich = getPrimeMeridianInfo(gtgp.pm)
      gtgp.pmLongToGreenwich = optPMLongToGreenwich getOrElse gtgp.pmLongToGreenwich
    } else {
      gtgp.pmLongToGreenwich =
        (geoKeyDirectory |-> geogPrimeMeridianLongLens get) getOrElse gtgp.pmLongToGreenwich

      gtgp.pmLongToGreenwich = angleToDD(gtgp.pmLongToGreenwich, gtgp.angle)
    }

    gtgp.length = (geoKeyDirectory |-> projLinearUnitsLens get) getOrElse UserDefinedCPV

    if (gtgp.length != UserDefinedCPV) {
      val optLengthInMeters = getLengthInfo(gtgp.length)
      gtgp.lengthInMeters = optLengthInMeters getOrElse gtgp.lengthInMeters
    } else gtgp.lengthInMeters =
      (geoKeyDirectory |-> projLinearUnitSizeLens get) getOrElse gtgp.lengthInMeters

    val projCoordTrans = (geoKeyDirectory |-> projCoordTransLens get)

    if (projCoordTrans != -1) {
      gtgp.ctProjection = projCoordTrans

      setProjectionParameters(gtgp)
    }

    getMapSystemAndZone(gtgp.projCode) match {
      case Some((mapSystem, zone)) => {
        gtgp.zone = zone
        gtgp.mapSystem = mapSystem
      }
      case None => Unit
    }

    if ((gtgp.mapSystem == MapSys_UTM_North || gtgp.mapSystem == MapSys_UTM_South)
      && gtgp.ctProjection == UserDefinedCPV) {
      gtgp.ctProjection = CT_TransverseMercator

      gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

      gtgp.projectionParameters(0) = (ProjNatOriginLatGeoKey, 0.0)
      gtgp.projectionParameters(1) = (ProjNatOriginLongGeoKey, gtgp.zone * 6 - 183.0)
      gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, 0.9996)
      gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, 500000.0)
      gtgp.projectionParameters(6) = (
        ProjFalseNorthingGeoKey,
        if (gtgp.mapSystem == MapSys_UTM_North) 0.0 else 10000000.0
      )
    }

    gtgp
  }

  private def getPCSData(pcs: Int, gtgp: GeoTiffGDALParameters) {
    val (optDatum, optZone, optMapSystem) = pcsToDatumZoneAndMapSystem(pcs)

    val optDatumName = optMapSystem match {
      case Some(mapSystem) if (mapSystem == MapSys_UTM_North
        || mapSystem == MapSys_UTM_South) => optDatum match {
        case Some(Datum_North_American_Datum_1927) => Some("NAD27")
        case Some(Datum_North_American_Datum_1983) => Some("NAD83")
        case Some(Datum_WGS72) => Some("WGS 72")
        case Some(Datum_WGS72_Transit_Broadcast_Ephemeris) => Some("WGS 72BE")
        case Some(Datum_WGS84) => Some("WGS 84")
        case _ => None
      }
      case _ => None
    }

    optDatumName match {
      case Some(datumName) => {
        val mapSystem = optMapSystem.get
        val mapSystemValue =
          if (mapSystem == MapSys_UTM_North) Proj_UTM_zone_1N
          else Proj_UTM_zone_1S

        gtgp.projCode = mapSystemValue - 1 + optZone.get

        gtgp.length = LinearMeterCode

        gtgp.gcs = optDatum.get
      }
      case None => {
        val optValueMap = csvReader.getPCVEPSGValues(pcs)

        if (!optValueMap.isEmpty) {
          val valueMap = optValueMap.get

          gtgp.length = valueMap.get(UOMCode) match {
            case Some(v) => v.toInt
            case None => gtgp.length
          }

          valueMap.get(CoordOpCode) match {
            case Some(v) if (v.toInt > 0) => gtgp.projCode = v.toInt
            case None => gtgp.length = UserDefinedCPV
          }

          valueMap.get(SourceGeoCRSCode) match {
            case Some(v) if (v.toInt > 0) => gtgp.gcs = v.toInt
            case None => gtgp.gcs = UserDefinedCPV
          }
        }
      }
    }
  }

  private def pcsToDatumZoneAndMapSystem(pcs: Int) = {
    var (optDatum, optMapSystem, optZone) =
      if (pcs >= PCS_NAD27_UTM_zone_3N && pcs <= PCS_NAD27_UTM_zone_22N)
        (
          Some(Datum_North_American_Datum_1983),
          Some(MapSys_UTM_North),
          Some(pcs - PCS_NAD27_UTM_zone_3N + 3)
        )
      else if (pcs >= PCS_NAD83_UTM_zone_3N && pcs <= PCS_NAD83_UTM_zone_23N)
        (
          Some(Datum_North_American_Datum_1983),
          Some(MapSys_UTM_North),
          Some(pcs - PCS_NAD83_UTM_zone_3N + 3)
        )
      else if (pcs >= PCS_WGS72_UTM_zone_1N && pcs <= PCS_WGS72_UTM_zone_60N)
        (Some(Datum_WGS72), Some(MapSys_UTM_North), Some(pcs - PCS_WGS72_UTM_zone_1N + 1))
      else if (pcs >= PCS_WGS72_UTM_zone_1S && pcs <= PCS_WGS72_UTM_zone_60S)
        (Some(Datum_WGS72), Some(MapSys_UTM_South), Some(pcs - PCS_WGS72_UTM_zone_1S + 1))
      else if (pcs >= PCS_WGS72BE_UTM_zone_1N && pcs <= PCS_WGS72BE_UTM_zone_60N)
        (Some(Datum_WGS72_Transit_Broadcast_Ephemeris), Some(MapSys_UTM_North), Some(pcs - PCS_WGS72BE_UTM_zone_1N + 1))
      else if (pcs >= PCS_WGS72BE_UTM_zone_1S && pcs <= PCS_WGS72BE_UTM_zone_60S)
        (Some(Datum_WGS72_Transit_Broadcast_Ephemeris), Some(MapSys_UTM_South), Some(pcs - PCS_WGS72BE_UTM_zone_1S + 1))
      else if (pcs >= PCS_WGS84_UTM_zone_1N && pcs <= PCS_WGS84_UTM_zone_60N)
        (Some(Datum_WGS84), Some(MapSys_UTM_North), Some(pcs - PCS_WGS84_UTM_zone_1N + 1))
      else if (pcs >= PCS_WGS84_UTM_zone_1S && pcs <= PCS_WGS84_UTM_zone_60S)
        (Some(Datum_WGS84), Some(MapSys_UTM_South), Some(pcs - PCS_WGS84_UTM_zone_1S + 1))
      else if (pcs >= PCS_SAD69_UTM_zone_18N && pcs <= PCS_SAD69_UTM_zone_22N)
        (Some(UserDefinedCPV), Some(MapSys_UTM_North), Some(pcs - PCS_SAD69_UTM_zone_18N + 18))
      else if (pcs >= PCS_SAD69_UTM_zone_17S && pcs <= PCS_SAD69_UTM_zone_25S)
        (Some(UserDefinedCPV), Some(MapSys_UTM_South), Some(pcs - PCS_SAD69_UTM_zone_17S + 17))
      else (None, None, None)

    val newPCS = projectionTypesMap.getOrElse(pcs, pcs)

    if (newPCS <= 15900 && newPCS >= 10000) {
      if ((newPCS % 100) >= 30) {
        optMapSystem = Some(MapSys_State_Plane_83)
        optDatum = Some(Datum_North_American_Datum_1983)
        optZone = Some(newPCS - 10000)
      } else {
        optMapSystem = Some(MapSys_State_Plane_27)
        optDatum = Some(Datum_North_American_Datum_1927)
        optZone = Some(newPCS - 10000 - 30)
      }
    }

    (optDatum, optZone, optMapSystem)
  }

  private def mapSystemToProjection(mapSystem: Int, zone: Int) =
    if (mapSystem == MapSys_UTM_North) Proj_UTM_zone_1N + zone - 1
    else if (mapSystem == MapSys_UTM_South) Proj_UTM_zone_1S + zone - 1
    else if (mapSystem == MapSys_State_Plane_27) {
      if (zone == 4100) 15302
      else 10000 + zone
    } else if (mapSystem == MapSys_State_Plane_83) {
      if (zone == 1601) 15303
      else 10000 + zone + 30
    } else UserDefinedCPV

  private def getProjectionFromTransformationCode(trfCode: Int) =
    if ((trfCode >= Proj_UTM_zone_1N && trfCode <= Proj_UTM_zone_60N) ||
      (trfCode >= Proj_UTM_zone_1S && trfCode <= Proj_UTM_zone_60S)) {
      val (north, zone) =
        if (trfCode <= Proj_UTM_zone_60N) (true, trfCode - Proj_UTM_zone_1N + 1)
        else (false, trfCode - Proj_UTM_zone_1S + 1)

      val projectionParameters = Array.ofDim[(Int, Double)](7)
      projectionParameters(0) = (UserDefinedCPV, 0)
      projectionParameters(1) = (UserDefinedCPV, -183 + 6 * zone)
      projectionParameters(2) = (UserDefinedCPV, 0)
      projectionParameters(3) = (UserDefinedCPV, 0)
      projectionParameters(4) = (UserDefinedCPV, 0.9996)
      projectionParameters(5) = (UserDefinedCPV, 500000)
      projectionParameters(6) = (UserDefinedCPV, if (north) 0 else 10000000)

      (Some(9807), Some(projectionParameters))
    } else (None, None)

  private def epsgProjMethodToCTProjMethod(epsg: Int) =
    projMethodToCTProjMethodMap.getOrElse(epsg, epsg)

  private def setGTParameterIdentities(ctProjection: Int,
    projParms: Array[(Int, Double)]) = ctProjection match {
    case CT_CassiniSoldner | CT_NewZealandMapGrid => {
      projParms(0) = (ProjNatOriginLatGeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjNatOriginLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_ObliqueMercator | CT_HotineObliqueMercatorAzimuthCenter => {
      projParms(0) = (ProjCenterLatGeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjCenterLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(2) = (ProjAzimuthAngleGeoKey, getValueIfNotNull(projParms(2)))
      projParms(3) = (ProjRectifiedGridAngleGeoKey, getValueIfNotNull(projParms(3)))
      projParms(4) = (ProjScaleAtCenterGeoKey, getValueIfNotNull(projParms(4)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_ObliqueMercator_Laborde => {
      projParms(0) = (ProjCenterLatGeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjCenterLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(2) = (ProjAzimuthAngleGeoKey, getValueIfNotNull(projParms(2)))
      projParms(4) = (ProjScaleAtCenterGeoKey, getValueIfNotNull(projParms(4)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_LambertConfConic_1SP | CT_Mercator
       | CT_ObliqueStereographic | CT_PolarStereographic
       | CT_TransverseMercator | CT_TransvMercator_SouthOriented => {
         projParms(0) = (ProjNatOriginLatGeoKey, getValueIfNotNull(projParms(0)))
         projParms(1) = (ProjNatOriginLongGeoKey, getValueIfNotNull(projParms(1)))
         projParms(4) = (ProjScaleAtNatOriginGeoKey, getValueIfNotNull(projParms(4)))
         projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
         projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
       }
    case CT_LambertConfConic_2SP => {
      projParms(0) = (ProjFalseOriginLatGeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjFalseOriginLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(2) = (ProjStdParallel1GeoKey, getValueIfNotNull(projParms(2)))
      projParms(3) = (ProjStdParallel2GeoKey, getValueIfNotNull(projParms(3)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_AlbersEqualArea => {
      projParms(0) = (ProjStdParallel1GeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjStdParallel2GeoKey, getValueIfNotNull(projParms(1)))
      projParms(2) = (ProjNatOriginLatGeoKey, getValueIfNotNull(projParms(2)))
      projParms(3) = (ProjNatOriginLongGeoKey, getValueIfNotNull(projParms(3)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_SwissObliqueCylindrical | CT_LambertAzimEqualArea => {
      projParms(0) = (ProjCenterLatGeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjCenterLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case CT_CylindricalEqualArea => {
      projParms(0) = (ProjStdParallel1GeoKey, getValueIfNotNull(projParms(0)))
      projParms(1) = (ProjNatOriginLongGeoKey, getValueIfNotNull(projParms(1)))
      projParms(5) = (ProjFalseEastingGeoKey, getValueIfNotNull(projParms(5)))
      projParms(6) = (ProjFalseNorthingGeoKey, getValueIfNotNull(projParms(6)))
    }
    case _ => Unit
  }

  private def getValueIfNotNull(tuple: (Int, Double)) =
    if (tuple == null) Double.NaN else tuple._2

  private def getGCSInfo(gcs: Int) = {
    val datum = gcs match {
      case GCS_NAD27 => Some(Datum_North_American_Datum_1927)
      case GCS_NAD83 => Some(Datum_North_American_Datum_1983)
      case GCS_WGS_84 => Some(Datum_WGS84)
      case GCS_WGS_72 => Some(Datum_WGS72)
      case GCS_WGS_72BE => Some(Datum_WGS72_Transit_Broadcast_Ephemeris)
      case _ => None
    }

    datum match {
      case Some(d) => (Some(PM_Greenwich), Some(Angular_DMS_Hemisphere), Some(d))
      case None => csvReader.getGCSEPSGValues(gcs) match {
        case Some(map) =>
          (
            map.get(PrimeMeridianCode).map(_.toInt),
            map.get(PrimeMeridianCode).map(_.toInt),
            map.get(UOMCode).map(_.toInt)
          )
        case None => (None, None, None)
      }
    }
  }

  private def getAngleInfo(angleCode: Int) = angleCode match {
    case 9101 => Some(180.0 / math.Pi)
    case 9102 | 9107 | 9108 | 9110 | 9122 => Some(1.0)
    case 9103 => Some(1 / 6.0)
    case 9104 => Some(1 / 3600.0)
    case 9105 => Some(180 / 200.0)
    case 9106 => Some(180 / 200.0)
    case 9109 => Some(180.0 / (math.Pi * 1000000.0))
    case _ => csvReader.getUnitOfMeasureValues(angleCode) match {
      case Some(map) => map.get(FactorBCode) match {
        case Some(factorB) if (factorB != "") => map.get(FactorCCode) match {
          case Some(factorC) if (factorC != "" && factorC.toDouble != 0.0) =>
            Some((factorB.toDouble / factorC.toDouble) * 180.0 / math.Pi)
        }
        case None => None
      }
      case None => None
    }
  }

  private def getDatumInfo(datum: Int) = datum match {
    case Datum_North_American_Datum_1927 => Some(Ellipse_Clarke_1866)
    case Datum_North_American_Datum_1983 => Some(Ellipse_GRS_1980)
    case Datum_WGS84 => Some(Ellipse_WGS_84)
    case Datum_WGS72 => Some(7043)
    case _ => csvReader.getDatumValues(datum) match {
      case Some(map) => map.get(EllipsoidCode) match {
        case Some(ellipsoidStr) if (ellipsoidStr != "") => Some(ellipsoidStr.toInt)
        case _ => None
      }
      case None => None
    }
  }

  private def semiMinorComp(semiMajor: Double, invFlattening: Double) =
    semiMajor * (1 - 1.0 / invFlattening)

  private def getEllipsoidInfo(ellipsoid: Int) = {
    val ellipsoidTuple = ellipsoid match {
      case Ellipse_Clarke_1866 => Some(6378206.4, 6356583.8, 0.0)
      case Ellipse_GRS_1980 => Some(6378137.0, 0.0, 298.257222101)
      case Ellipse_WGS_84 => Some(6378137.0, 0.0, 298.257223563)
      case 7043 => Some(6378135.0, 0.0, 298.26)
      case _ => None
    }

    ellipsoidTuple match {
      case Some((semiMajor, semiMinor, invFlattening)) =>
        if (semiMinor == 0.0)
          (Some(semiMajor), Some(semiMinorComp(semiMajor, invFlattening)))
        else
          (Some(semiMajor), Some(semiMinor))
      case None => csvReader.getEllipsoidValues(ellipsoid) match {
        case Some(map) => map.get(SemiMajorAxisCode) match {
          case Some(semiMajorStr) =>
            if (semiMajorStr != "" && semiMajorStr.toDouble != 0.0) {
              val semiMajor = map.get(UOMCode) match {
                case Some(code) if (code != "") => getLengthInfo(code.toInt) match {
                  case Some(conv) => semiMajorStr.toDouble * conv
                  case None => semiMajorStr.toDouble
                }
                case None => Double.NaN
              }

              if (semiMajor == Double.NaN) (None, None)
              else {
                map.get(SemiMinorAxisCode) match {
                  case Some(semiMinorStr) if (semiMinorStr != "") => {
                    val semiMinor = semiMinorStr.toDouble

                    if (semiMinor == 0.0) map.get(InvFlatteningCode) match {
                      case Some(invFlatteningStr) if (invFlatteningStr != "") =>
                        (
                          Some(semiMajor),
                          Some(semiMinorComp(semiMajor, invFlatteningStr.toDouble))
                        )
                      case None => (Some(semiMajor), None)
                    } else (Some(semiMajor), Some(semiMinor))
                  }
                  case None => (Some(semiMajor), None)
                }
              }
            } else (None, None)
          case None => (None, None)
        }
        case None => (None, None)
      }
    }
  }

  private def getLengthInfo(lengthCode: Int) = lengthCode match {
    case 9001 => Some(1.0)
    case 9002 => Some(0.3048)
    case 9003 => Some(12.0 / 39.37)
    case _ => csvReader.getUnitOfMeasureValues(lengthCode) match {
      case Some(map) => (map.get(FactorBCode), map.get(FactorCCode)) match {
        case (Some(factorB), Some(factorC)) if (factorC != "" && factorC.toDouble > 0.0) =>
          Some(factorB.toDouble / factorC.toDouble)
        case _ => Some(0.0)
      }
      case None => None
    }
  }

  private def getPrimeMeridianInfo(pm: Int): Option[Double] = pm match {
    case PM_Greenwich => Some(0.0)
    case _ => csvReader.getPrimeMeridianValues(pm) match {
      case Some(map) => map.get(UOMCode) match {
        case Some(uomAngleStr) if (uomAngleStr != "") => map.get(GreenwichLongitudeCode) match {
          case Some(angleString) => Some(angleStringToDD(angleString, uomAngleStr.toInt))
          case None => None
        }
        case None => None
      }
      case None => None
    }
  }

  private def angleStringToDD(angleString: String, angleCode: Int): Double =
    if (angleCode == 9110) {
      var angle: Double = math.abs(angleString.toInt)
      val decimals = angleString.dropWhile(_ != '.').filter(_ != '.')

      if (!decimals.isEmpty) {
        val minutes = new StringBuilder()
        minutes.append(decimals(0))

        if (decimals.size > 1) minutes.append(decimals(1))
        angle += minutes.toString.toDouble / 60.0

        if (decimals.size > 2) {
          val seconds = new StringBuilder()
          seconds.append(decimals(3))
          if (decimals.size > 3 && decimals(4) >= '0' && decimals(4) <= '9') {
            seconds.append(decimals(4))
            seconds.append('.')
            seconds.append(decimals.slice(5, decimals.size))
          } else seconds.append('0')

          angle += seconds.toString.toDouble / 3600.0
        }
      }

      math.abs(angle)
    }
    else if (angleCode == 9105 || angleCode == 9106) 180 * (angleString.toDouble / 200)
    else if (angleCode == 9101) 180 * (angleString.toDouble / math.Pi)
    else if (angleCode == 9103) angleString.toDouble / 60
    else if (angleCode == 9104) angleString.toDouble / 3600
    else if (angleCode == 0 || angleCode == UserDefinedCPV || angleCode == 0)
      throw new MalformedGeoTiffException("Angle must be set.")
    else angleString.toDouble

  private def angleToDD(angle: Double, angleCode: Int) =
    if (angleCode == 9110) angleStringToDD(angle.toString, angleCode)
    else if (angleCode != UserDefinedCPV) getAngleInfo(angleCode) match {
      case Some(pm) => pm * angle
      case None => throw new MalformedGeoTiffException("Angle must be set.")
    }
    else angle

  private def setProjectionParameters(gtgp: GeoTiffGDALParameters) {
    var originLong, originLat, rectGridAngle = 0.0
    var falseEasting, falseNorthing = 0.0
    var originScale = 1.0
    var stdParallel1, stdParallel2, azimuth = 0.0

    val eastingList = List(
      geoKeyDirectory |-> projFalseEastingLens get,
      geoKeyDirectory |-> projCenterEastingLens get,
      geoKeyDirectory |-> projFalseOriginEastingLens get
    )

    falseEasting = getOptDoubleValue(eastingList, 0.0)

    val northingList = List(
      geoKeyDirectory |-> projFalseNorthingLens get,
      geoKeyDirectory |-> projCenterNorthingLens get,
      geoKeyDirectory |-> projFalseOriginNorthingLens get
    )

    falseNorthing = getOptDoubleValue(northingList, 0.0)

    def setOriginLong = {
      val originLongList = List(
        geoKeyDirectory |-> projNatOriginLongLens get,
        geoKeyDirectory |-> projFalseOriginLongLens get,
        geoKeyDirectory |-> projCenterLongLens get
      )

      originLong = getOptDoubleValue(originLongList, 0.0)
    }

    def setOriginLat = {
      val originLatList = List(
        geoKeyDirectory |-> projNatOriginLatLens get,
        geoKeyDirectory |-> projFalseOriginLatLens get,
        geoKeyDirectory |-> projCenterLatLens get
      )

      originLat = getOptDoubleValue(originLatList, 0.0)
    }

    def setOriginScaleNatOrigin =
      originScale = (geoKeyDirectory |-> projScaleAtNatOriginLens get) getOrElse 1.0

    def setOriginScale = {
      setOriginScaleNatOrigin

      if (originScale == 1.0)
        originScale = (geoKeyDirectory |-> projScaleAtCenterLens get) getOrElse 1.0
    }

    gtgp.ctProjection match {
      case CT_Stereographic => {
        setOriginLong
        setOriginLat
        setOriginScaleNatOrigin

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjCenterLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjCenterLongGeoKey, originLong)
        gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, originScale)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_LambertConfConic_1SP | CT_Mercator |
          CT_ObliqueStereographic | CT_TransverseMercator |
          CT_TransvMercator_SouthOriented => {
            setOriginLong
            setOriginLat
            setOriginScaleNatOrigin

            gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

            gtgp.projectionParameters(0) = (ProjNatOriginLatGeoKey, originLat)
            gtgp.projectionParameters(1) = (ProjNatOriginLongGeoKey, originLong)
            gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, originScale)
            gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
            gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
          }
      case CT_ObliqueMercator | CT_HotineObliqueMercatorAzimuthCenter => {
        setOriginLong
        setOriginLat
        azimuth = (geoKeyDirectory |-> projAzimuthAngleLens get) getOrElse 0.0
        rectGridAngle = (geoKeyDirectory |-> projRectifiedGridAngleLens get) getOrElse 90.0
        setOriginScale

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjCenterLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjCenterLongGeoKey, originLong)
        gtgp.projectionParameters(2) = (ProjAzimuthAngleGeoKey, azimuth)
        gtgp.projectionParameters(3) = (ProjRectifiedGridAngleGeoKey, rectGridAngle)
        gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, originScale)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_CassiniSoldner | CT_Polyconic => {
        setOriginLong
        setOriginLat
        setOriginScale

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjNatOriginLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjNatOriginLongGeoKey, originLong)
        gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, originScale)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_AzimuthalEquidistant | CT_MillerCylindrical |
          CT_Gnomonic | CT_LambertAzimEqualArea | CT_Orthographic |
          CT_NewZealandMapGrid => {
            setOriginLong
            setOriginLat

            gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

            gtgp.projectionParameters(0) = (ProjCenterLatGeoKey, originLat)
            gtgp.projectionParameters(1) = (ProjCenterLongGeoKey, originLong)
            gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
            gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
          }
      case CT_Equirectangular => {
        setOriginLong
        setOriginLat
        stdParallel1 = (geoKeyDirectory |-> projStdParallel1Lens get) getOrElse 0.0

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjCenterLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjCenterLongGeoKey, originLong)
        gtgp.projectionParameters(2) = (ProjStdParallel1GeoKey, stdParallel1)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_Robinson | CT_Sinusoidal | CT_VanDerGrinten => {
        setOriginLong

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(1) = (ProjCenterLongGeoKey, originLong)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_PolarStereographic => {
        originLong = (geoKeyDirectory |-> projStraightVertPoleLongLens get) getOrElse 0.0

        if (originLong == 0.0) setOriginLong
        setOriginLat
        setOriginScale

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjNatOriginLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjStraightVertPoleLongGeoKey, originLong)
        gtgp.projectionParameters(4) = (ProjScaleAtNatOriginGeoKey, originScale)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_LambertConfConic_2SP => {
        stdParallel1 = (geoKeyDirectory |-> projStdParallel1Lens get) getOrElse 0.0
        stdParallel2 = (geoKeyDirectory |-> projStdParallel2Lens get) getOrElse 0.0

        setOriginLong
        setOriginLat

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjFalseOriginLatGeoKey, originLat)
        gtgp.projectionParameters(1) = (ProjFalseOriginLongGeoKey, originLong)
        gtgp.projectionParameters(2) = (ProjStdParallel1GeoKey, stdParallel1)
        gtgp.projectionParameters(3) = (ProjStdParallel2GeoKey, stdParallel2)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_AlbersEqualArea | CT_EquidistantConic => {
        stdParallel1 = (geoKeyDirectory |-> projStdParallel1Lens get) getOrElse 0.0
        stdParallel2 = (geoKeyDirectory |-> projStdParallel2Lens get) getOrElse 0.0

        setOriginLong
        setOriginLat

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjStdParallel1GeoKey, stdParallel1)
        gtgp.projectionParameters(1) = (ProjStdParallel2GeoKey, stdParallel2)
        gtgp.projectionParameters(2) = (ProjNatOriginLatGeoKey, originLat)
        gtgp.projectionParameters(3) = (ProjNatOriginLongGeoKey, originLong)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case CT_CylindricalEqualArea => {
        stdParallel1 = (geoKeyDirectory |-> projStdParallel1Lens get) getOrElse 0.0

        setOriginLong

        gtgp.projectionParameters = Array.ofDim[(Int, Double)](7)

        gtgp.projectionParameters(0) = (ProjStdParallel1GeoKey, stdParallel1)
        gtgp.projectionParameters(1) = (ProjNatOriginLongGeoKey, originLong)
        gtgp.projectionParameters(5) = (ProjFalseEastingGeoKey, falseEasting)
        gtgp.projectionParameters(6) = (ProjFalseNorthingGeoKey, falseNorthing)
      }
      case _ => Unit
    }

    for (i <- 0 until gtgp.projectionParameters.size) {
      val v = gtgp.projectionParameters(i)

      if (v != null) v._1 match {
        case ProjFalseEastingGeoKey | ProjFalseNorthingGeoKey |
            ProjFalseOriginEastingGeoKey | ProjFalseOriginNorthingGeoKey |
            ProjCenterEastingGeoKey | ProjCenterNorthingGeoKey =>
          if (gtgp.lengthInMeters != 0.0 && gtgp.lengthInMeters != 1.0) {
            gtgp.projectionParameters(i) = (v._1, v._2 * gtgp.lengthInMeters)
          }
        case _ => Unit
      }
    }
  }

  private def getOptDoubleValue(opts: List[Option[Double]], default: Double) =
    opts.filter(_ != None).headOption match {
      case Some(head) if (!head.isEmpty) => head.get
      case None => default
    }

  private def getMapSystemAndZone(projCode: Int) =
    if (projCode >= Proj_UTM_zone_1N && projCode <= Proj_UTM_zone_60N)
      Some((MapSys_UTM_North, projCode - Proj_UTM_zone_1N + 1))
    else if (projCode >= Proj_UTM_zone_1S && projCode <= Proj_UTM_zone_60S)
      Some((MapSys_UTM_South, projCode - Proj_UTM_zone_1S + 1))
    else if (projCode >= 10101 && projCode <= 15299) {
      if (projCode % 100 >= 30) Some(MapSys_State_Plane_83, projCode - 10030)
      else Some((MapSys_State_Plane_27, projCode - 10000))
    } else None

  private def getProj4String(gtgp: GeoTiffGDALParameters): Option[String] = {
    val proj4SB = new StringBuilder

    val units = projectedLinearUnitsMap.get(gtgp.length) match {
      case Some(unit) => s" +units=$unit"
      case None => s" +to_meter=${gtgp.lengthInMeters}"
    }

    val falseEastingParams =
      if (gtgp.projectionParameters.size >= 6) Some(gtgp.projectionParameters(5))
      else None
    val falseNorthingParams =
      if (gtgp.projectionParameters.size >= 7) Some(gtgp.projectionParameters(6))
      else None

    val falseEasting = falseEastingParams match {
      case Some(tup) => tup._2
      case None => 0.0
    }

    val falseNorthing = falseNorthingParams match {
      case Some(tup) => tup._2
      case None => 0.0
    }

    if (gtgp.model == ModelTypeGeographic)
      proj4SB.append("+proj=latlong")
    else if (gtgp.mapSystem == MapSys_UTM_North)
      proj4SB.append(s"+proj=utm +zone=${gtgp.zone}")
    else if (gtgp.ctProjection == CT_TransverseMercator) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val k = gtgp.projectionParameters(4)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=tmerc +lat_0=$lat_0 +lon_0=$lon_0 +k=$k +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_CassiniSoldner) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=cass +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_ObliqueStereographic) {
      val lat_ts = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val k = gtgp.projectionParameters(4)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=stere +lat_ts=$lat_ts +lon_0=$lon_0 +k=$k +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Stereographic) {
      val lat_ts = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=stere +lat_ts=$lat_ts +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_PolarStereographic) {
      val lat_ts = gtgp.projectionParameters(0)._2
      val lat_0 = if (lat_ts > 0.0) "90" else "-90"
      val lon_0 = gtgp.projectionParameters(1)._2
      val k = gtgp.projectionParameters(4)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=stere +lat_0=$lat_0 +lat_ts=$lat_ts +lon_0=$lon_0 +k=$k +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Equirectangular) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val lat_ts = gtgp.projectionParameters(2)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=eqc +lat_0=$lat_0 +lon_0=$lon_0 +lat_ts=$lat_ts +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Gnomonic) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=gnom +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Orthographic) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=orth +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_LambertAzimEqualArea) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=laea +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_AzimuthalEquidistant) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=aeqd +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_MillerCylindrical) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=mill +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Polyconic) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=poly +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_AlbersEqualArea) {
      val lat_1 = gtgp.projectionParameters(0)._2
      val lat_2 = gtgp.projectionParameters(1)._2
      val lat_0 = gtgp.projectionParameters(2)._2
      val lon_0 = gtgp.projectionParameters(3)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=aea +lat_1=$lat_1 +lat_2=$lat_2 +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_EquidistantConic) {
      val lat_1 = gtgp.projectionParameters(0)._2
      val lat_2 = gtgp.projectionParameters(1)._2
      val lat_0 = gtgp.projectionParameters(2)._2
      val lon_0 = gtgp.projectionParameters(3)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=eqdc +lat_1=$lat_1 +lat_2=$lat_2 +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Robinson) {
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=robin +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_VanDerGrinten) {
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=vandg +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_Sinusoidal) {
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=sinu +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_LambertConfConic_2SP) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val lat_1 = gtgp.projectionParameters(2)._2
      val lat_2 = gtgp.projectionParameters(3)._2
      val x_0 = falseEasting
      val y_0 = falseNorthing

      proj4SB.append(
        s"+proj=lcc +lat_0=$lat_0 +lon_0=$lon_0 +lat_1=$lat_1 +lat_2=$lat_2 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_LambertConfConic_1SP) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lat_1 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val k = gtgp.projectionParameters(4)._2
      val x_0 = gtgp.projectionParameters(5)._2
      val y_0 = gtgp.projectionParameters(6)._2

      proj4SB.append(
        s"+proj=lcc +lat_0=$lat_0 +lat_1=$lat_1 +lon_0=$lon_0 +k=$k +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_CylindricalEqualArea) {
      val lat_ts = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = gtgp.projectionParameters(5)._2
      val y_0 = gtgp.projectionParameters(6)._2

      proj4SB.append(
        s"+proj=cea +lat_ts=$lat_ts +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_NewZealandMapGrid) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lon_0 = gtgp.projectionParameters(1)._2
      val x_0 = gtgp.projectionParameters(5)._2
      val y_0 = gtgp.projectionParameters(6)._2

      proj4SB.append(
        s"+proj=nzmg +lat_0=$lat_0 +lon_0=$lon_0 +x_0=$x_0 +y_0=$y_0"
      )
    } else if (gtgp.ctProjection == CT_ObliqueMercator) {
      val lat_0 = gtgp.projectionParameters(0)._2
      val lonc = gtgp.projectionParameters(1)._2
      val alpha = gtgp.projectionParameters(2)._2
      val k = gtgp.projectionParameters(4)._2
      val x_0 = gtgp.projectionParameters(5)._2
      val y_0 = gtgp.projectionParameters(6)._2

      proj4SB.append(
        s"+proj=omerc +lat_0=$lat_0 +lonc=$lonc +alpha=$alpha +k=$k +x_0=$x_0 +y_0=$y_0"
      )
    }

    if (gtgp.datum == Datum_WGS84)
      proj4SB.append(" +datum=WGS84")
    else if (gtgp.datum == Datum_WGS72_Transit_Broadcast_Ephemeris)
      proj4SB.append(" +datum=WGS72BE")
    else if (gtgp.datum == Datum_WGS72)
      proj4SB.append(" +datum=WGS72")
    else if (gtgp.datum == Datum_North_American_Datum_1927)
      proj4SB.append(" +datum=NAD27")
    else if (gtgp.datum == Datum_North_American_Datum_1983)
      proj4SB.append(" +datum=NAD83")
    else if (gtgp.ellipsoid == Ellipse_WGS_84)
      proj4SB.append(" +ellps=WGS84")
    else if (gtgp.ellipsoid == Ellipse_Clarke_1866)
      proj4SB.append(" +ellps=clrk66")
    else if (gtgp.ellipsoid == Ellipse_Clarke_1880)
      proj4SB.append(" +ellps=clrk80")
    else if (gtgp.ellipsoid == Ellipse_Clarke_1880)
      proj4SB.append(" +ellps=clrk80")
    else if (gtgp.ellipsoid == Ellipse_GRS_1980)
      proj4SB.append(" +ellps=GRS80")
    else if (gtgp.semiMinor != 0.0 && gtgp.semiMajor != 0.0) {
      proj4SB.append(s" +a=${gtgp.semiMajor} +b=${gtgp.semiMinor}")
    }

    if (proj4SB.length == 0 ||
      gtgp.ctProjection == CT_TransvMercator_SouthOriented) None
    else Some(proj4SB.append(units).toString)
  }

}
