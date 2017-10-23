/*
 * Copyright 2016 Azavea
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

package geotrellis.raster.io.geotiff.writer

import collection.immutable.Map

import collection.mutable.ListBuffer

import geotrellis.proj4.CRS

import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags._
import DatumTypes._
import GeographicCSTypes._
import EllipsoidTypes._
import ProjectionTypesMap._
import CommonPublicValues._
import GeoKeys._
import ModelTypes._
import CoordinateTransformTypes._
import ProjectedLinearUnits._
import AngularUnitTypes.Angular_Degree

case class GeoDirectoryTags(shortTags: Array[(Int, Int, Int, Int)], doubles: Array[Double])

object CoordinateSystemParser {

  val GeoTiffDoubleTag = 0x87b0

  def apply(crs: CRS, pixelSampleType: Option[PixelSampleType]): CoordinateSystemParser =
    new CoordinateSystemParser(crs, pixelSampleType)

  def parse(crs: CRS, pixelSampleType: Option[PixelSampleType]): GeoDirectoryTags = {
    val (s, d) = apply(crs, pixelSampleType).parse
    GeoDirectoryTags(s, d)
  }

}

class MalformedProj4Exception(message: String) extends RuntimeException(message)

class GeoTiffWriterLimitationException(message: String) extends RuntimeException(message)

class CoordinateSystemParser(val crs: CRS, val pixelSampleType: Option[PixelSampleType]) {

  import CoordinateSystemParser._

  private val proj4String: String = crs.toProj4String

  private val proj4Map: Map[String, String] =
    proj4String.split('+').
      map(_.trim).
      filter(_.contains('=')).
      groupBy(s => s.takeWhile(_ != '=')).
      map { case (a, b) => (a, b.head.dropWhile(_ != '=').substring(1)) }

  private lazy val (ellipsoid, optSemis): (Int, Option[(Double, Double, Double)]) =
    proj4Map.get("ellps") match {
      case Some("WGS84") => (Ellipse_WGS_84, None)
      case Some("clrk66") => (Ellipse_Clarke_1866, None)
      case Some("clrk80") => (Ellipse_Clarke_1880, None)
      case Some("GRS80") => (Ellipse_GRS_1980, None)
      case _ => {
        val ellps = UserDefinedCPV
        val major = getDouble("a")
        val minor = getDouble("b")

        val invFlattening = getDouble("rf")

        if (invFlattening == 0.0 && minor != 0.0)
          (ellps, Some(major, minor, -1 / (minor / major - 1)))
        else
          (ellps, Some(major, minor, invFlattening))
      }
    }

  private lazy val (gcs, datum): (Int, Int) = proj4Map.get("datum") match {
    case Some("WGS84") => (GCS_WGS_84, Datum_WGS84)
    case Some("NAD83") => (GCS_NAD83, Datum_North_American_Datum_1983)
    case Some("NAD27") => (GCS_NAD27, Datum_North_American_Datum_1927)
    case _ => (UserDefinedCPV, UserDefinedCPV)
  }

  // This method returns a tuple of the short geokeys and the double geokeys
  // to be written, sorted.
  lazy val parse: (Array[(Int, Int, Int, Int)], Array[Double]) = {
    val geoKeysIntBuffer = ListBuffer[(Int, Int)]()
    val doublesBuffer = ListBuffer[(Int, Double)]()

    pixelSampleType match {
      case Some(PixelIsPoint) =>
        geoKeysIntBuffer ++= List((GTRasterTypeGeoKey, 2))
      case _ =>
        // Default to PixelIsArea
        geoKeysIntBuffer ++= List((GTRasterTypeGeoKey, 1))
    }

    val epsgCode = crs.epsgCode.getOrElse(UserDefinedProjectionType)

    if (epsgCode != UserDefinedProjectionType) {
      val projPropsGeoKeysInt: List[(Int, Int)] =
        if(crs.isGeographic) {
          List(
            (GTModelTypeGeoKey, ModelTypeGeographic),
            (GeogTypeGeoKey, epsgCode)
          )
        } else {
          List(
            (GTModelTypeGeoKey, ModelTypeProjected),
            (GeogAngularUnitsGeoKey, 9102),
            (ProjectedCSTypeGeoKey, epsgCode)
          )
        }
      geoKeysIntBuffer ++= projPropsGeoKeysInt
    } else {
      val (projPropsGeoKeysInt, projPropsDoubles) = projProps
      geoKeysIntBuffer ++= projPropsGeoKeysInt
      doublesBuffer ++= projPropsDoubles

      val (gcsOrDatumGeoKeysInt, gcsOrDatumDoubles) = gcsOrDatumProps
      geoKeysIntBuffer ++= gcsOrDatumGeoKeysInt
      doublesBuffer ++= gcsOrDatumDoubles

      val (ellipsoidGeoKeysInt, ellipsoidDoubles) = ellipsoidProps
      geoKeysIntBuffer ++= ellipsoidGeoKeysInt
      doublesBuffer ++= ellipsoidDoubles
    }

    val (linearUnitsGeoKeysInt, linearUnitsDoubles) = linearUnitProps
    geoKeysIntBuffer ++= linearUnitsGeoKeysInt
    doublesBuffer ++= linearUnitsDoubles

    val doubles = doublesBuffer.toList.sortBy(_._1)
    val geoKeysInt = geoKeysIntBuffer.toList.map(x => (x._1, 0, 1, x._2))
    val geoKeys = (geoKeysInt ++
      doubles.zipWithIndex.map(x => (x._1._1, GeoTiffDoubleTag, 1, x._2))).sortBy(_._1)


    (geoKeys.toArray, doubles.map(_._2).toArray)
  }

  //TODO: There are more projections that need to be added.
  // GDAL parses off of WKT, and there are many missing from the proj4 parser
  // that this code is taken from. We need to be able to write out more projections.
  private lazy val projProps = proj4Map.get("proj") match {
    case Some("tmerc") => tmercProps
    case Some("utm") => utmProps
    case Some("lcc") => lccProps
    case Some("longlat") | Some("latlong") => longLatProps
    case Some("sinu") ⇒ sinuProps
    case Some("aea") => aeaProps
    case Some(p) => throw new GeoTiffWriterLimitationException(
      s"This GeoTiff writer does not currently support the projection $proj4String without an EPSG code associated with the CRS. You'll need to use a CRS that has an EPSG code, or reproject before writing to GeoTIFF."
    )
    case None => throw new MalformedProj4Exception(
      "No +proj flag specified."
    )
  }

  private lazy val tmercProps = {
    val geoKeysInt = List(
      (GTModelTypeGeoKey, ModelTypeProjected),
      (ProjectedCSTypeGeoKey, UserDefinedCPV),
      (ProjCoordTransGeoKey, CT_TransverseMercator)
    )

    val doubles = List(
      (ProjNatOriginLatGeoKey, getDouble("lat_0")),
      (ProjNatOriginLongGeoKey, getDouble("lon_0")),
      (ProjScaleAtNatOriginGeoKey, getK(1.0)),
      (ProjFalseEastingGeoKey, getDouble("x_0")),
      (ProjFalseNorthingGeoKey, getDouble("y_0"))
    )

    (geoKeysInt, doubles)
  }

  private lazy val utmProps = {
    val zone = getInt("zone")
    val south = proj4Map.contains("south")

    val epsgCodeBase = if (south) 32700 else 32600

    val geoKeysInt = List(
      (GTModelTypeGeoKey, ModelTypeProjected),
      (ProjectedCSTypeGeoKey, UserDefinedCPV),
      (ProjCoordTransGeoKey, CT_TransverseMercator),
      (ProjectedCSTypeGeoKey, epsgCodeBase + zone)
    )

    val doubles = List(
      (ProjNatOriginLatGeoKey, 0.0),
      (ProjNatOriginLongGeoKey, zone * 6 - 183.0),
      (ProjScaleAtNatOriginGeoKey, 0.9996),
      (ProjFalseEastingGeoKey, 500000.0),
      (ProjFalseNorthingGeoKey, if (south) 10000000.0 else 0.0)
    )

    (geoKeysInt, doubles)
  }

  private lazy val lccProps = {
    val lat0 = getDouble("lat_0")
    val lat1 = getDouble("lat_1")

    val geoKeysInt = List(
      (GTModelTypeGeoKey, ModelTypeProjected),
      (ProjectedCSTypeGeoKey, UserDefinedCPV),
      (ProjectionGeoKey, UserDefinedCPV),
      (ProjCoordTransGeoKey, CT_LambertConfConic_2SP)
    )

    val doublesLB = ListBuffer[(Int, Double)]()

    doublesLB += (ProjNatOriginLatGeoKey -> lat0)
    doublesLB += (ProjNatOriginLongGeoKey -> getDouble("lon_0"))

    if (lat0 == lat1) {
      doublesLB += (ProjScaleAtNatOriginGeoKey -> getK(1.0))
    } else {
      val lat2 = getDouble("lat_2")
      doublesLB += (ProjStdParallel1GeoKey -> lat1)
      doublesLB += (ProjStdParallel2GeoKey -> lat2)
    }

    doublesLB += (ProjFalseEastingGeoKey -> getDouble("x_0"))
    doublesLB += (ProjFalseNorthingGeoKey -> getDouble("y_0"))

    (geoKeysInt, doublesLB.toList)
  }

  private lazy val longLatProps =
    (List((GTModelTypeGeoKey, ModelTypeGeographic)), Nil)

  private lazy val gcsOrDatumProps = if (gcs != UserDefinedCPV) {
    (List((GeogTypeGeoKey, gcs)), Nil)
  } else {
    var geoKeysInt = List(
      (GeogTypeGeoKey, UserDefinedCPV),
      (GeogGeodeticDatumGeoKey, datum)
    )

    (geoKeysInt, Nil)
  }

  private lazy val ellipsoidProps = if (gcs == UserDefinedCPV) {
    val geoKeysInt = List((GeogEllipsoidGeoKey, ellipsoid))

    if (!optSemis.isEmpty) {
      val (major, minor, invFlattening) = optSemis.get

      val doubles = List(
        (GeogSemiMajorAxisGeoKey, major),
        (GeogSemiMinorAxisGeoKey, minor),
        (GeogInvFlatteningGeoKey, invFlattening)
      )

      (geoKeysInt, doubles)
    } else (geoKeysInt, Nil)
  } else (Nil, Nil)

  private lazy val sinuProps = {
    val geoKeysInt = List(
      (GTModelTypeGeoKey, ModelTypeProjected),
      (ProjectedCSTypeGeoKey, UserDefinedCPV),
      (ProjCoordTransGeoKey, CT_Sinusoidal),
      (ProjLinearUnitsGeoKey, LinearMeterCode),
      (GeogAngularUnitsGeoKey, Angular_Degree)
    )

    val doubles = List(
      (ProjNatOriginLatGeoKey, getDouble("lat_0")),
      (ProjNatOriginLongGeoKey, getDouble("lon_0")),
      (ProjFalseEastingGeoKey, getDouble("x_0")),
      (ProjFalseNorthingGeoKey, getDouble("y_0"))
    )

    (geoKeysInt, doubles)
  }

  private lazy val linearUnitProps = {
    val unitString = getString("units", "err")
    val code = reversedProjectedLinearUnitsMap.getOrElse(unitString, UserDefinedCPV)
    val geoKeysInt = List((ProjLinearUnitsGeoKey, code))

    val toMeters = getDouble("to_meter", "1.0")

    val doubles =
      if (code == UserDefinedCPV) List((ProjLinearUnitSizeGeoKey, toMeters))
      else Nil

    (geoKeysInt, doubles)
  }

  private lazy val aeaProps = {
    val lat0 = getDouble("lat_0")
    val lat1 = getDouble("lat_1")

    val geoKeysInt = List(
      (GTModelTypeGeoKey, ModelTypeProjected),
      (ProjectedCSTypeGeoKey, UserDefinedCPV),
      (ProjectionGeoKey, UserDefinedCPV),
      (ProjCoordTransGeoKey, CT_AlbersEqualArea)
    )

    val doublesLB = ListBuffer[(Int, Double)]()

    doublesLB += (ProjNatOriginLatGeoKey -> lat0)
    doublesLB += (ProjNatOriginLongGeoKey -> getDouble("lon_0"))

    if (lat0 == lat1) {
      doublesLB += (ProjScaleAtNatOriginGeoKey -> getK(1.0))
    } else {
      val lat2 = getDouble("lat_2")
      doublesLB += (ProjStdParallel1GeoKey -> lat1)
      doublesLB += (ProjStdParallel2GeoKey -> lat2)
    }

    doublesLB += (ProjFalseEastingGeoKey -> getDouble("x_0"))
    doublesLB += (ProjFalseNorthingGeoKey -> getDouble("y_0"))

    (geoKeysInt, doublesLB.toList)
  }


  private def getString(key: String, defV: String) =
    proj4Map.getOrElse(key, defV)

  private def getInt(key: String, defV: String = "0") =
    proj4Map.getOrElse(key, defV).toInt

  private def getDouble(key: String, defV: String = "0.0") =
    getString(key, defV).toDouble

  private def getK(defV: Double) = proj4Map.get("k") match {
    case Some(k) => k.toDouble
    case None => proj4Map.get("k_0") match {
      case Some(k) => k.toDouble
      case None => defV
    }
  }

}
