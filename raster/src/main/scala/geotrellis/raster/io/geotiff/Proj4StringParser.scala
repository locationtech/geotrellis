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

package geotrellis.raster.io.geotiff

import collection.immutable.Map

import geotrellis.raster.io.geotiff.reader.EllipsoidTypes._
import geotrellis.raster.io.geotiff.reader.DatumTypes._
import geotrellis.raster.io.geotiff.reader.GeographicCSTypes._
import geotrellis.raster.io.geotiff.reader.EllipsoidTypes._
import geotrellis.raster.io.geotiff.reader.CommonPublicValues._
import geotrellis.raster.io.geotiff.reader.GeoKeys._
import geotrellis.raster.io.geotiff.reader.ModelTypes._
import geotrellis.raster.io.geotiff.reader.CoordinateTransformTypes._
import geotrellis.raster.io.geotiff.reader.ProjectedLinearUnits._

object Proj4StringParser {

  def apply(proj4String: String): Proj4StringParser =
    new Proj4StringParser(proj4String)

}

class MalformedProj4Exception(message: String) extends RuntimeException(message)

class GeoTiffWriterLimitationException(message: String) extends RuntimeException(message)

class Proj4StringParser(val proj4String: String) {

  import Proj4StringParser._

  val proj4Map: Map[String, String] =
    proj4String.split('+').
      map(_.trim).
      filter(_.contains('=')).
      groupBy(s => s.takeWhile(_ != '=')).
      map { case (a, b) => (a, b.head.dropWhile(_ != '=').substring(1)) }

  lazy val (ellipsoid, optSemis): (Int, Option[(Double, Double, Double)]) =
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

  lazy val (gcs, datum): (Int, Int) = proj4Map.get("datum") match {
    case Some("WGS84") => (GCS_WGS_84, Datum_WGS84)
    case Some("NAD83") => (GCS_NAD83, Datum_North_American_Datum_1983)
    case Some("NAD27") => (GCS_NAD27, Datum_North_American_Datum_1927)
    case _ => (UserDefinedCPV, UserDefinedCPV)
  }

  //TODO: Fix more of these.
  def parseProjection(encoder: Encoder) = proj4Map.get("proj") match {
    case Some("tmerc") => setTMercProps(encoder)
    case Some("utm") => setUTMProps(encoder)
    case Some("lcc") => setLCCProps(encoder)
    case Some("longlat") | Some("latlong") => Unit
    case Some(p) => throw new GeoTiffWriterLimitationException(
      s"This GeoTiff writer does either not support the projection $p or it is malformed."
    )
    case None => throw new MalformedProj4Exception(
      "No +proj flag specified."
    )
  }
  // TODO: change CT_TransverseMercator to the EPSG value
  def setTMercProps(encoder: Encoder) {
    val pcKey = reversedProjMethodToCTProjMethodMap(CT_TransverseMercator)
    encoder.todoGeoKeyInt(GTModelTypeGeoKey, ModelTypeProjected)
    encoder.todoGeoKeyInt(ProjectedCSTypeGeoKey, UserDefinedCPV)
    encoder.todoGeoKeyInt(ProjCoordTransGeoKey, pcKey)
    encoder.todoGeoKeyDouble(ProjNatOriginLatGeoKey, getDouble("lat_0"))
    encoder.todoGeoKeyDouble(ProjNatOriginLongGeoKey, getDouble("lon_0"))
    encoder.todoGeoKeyDouble(ProjScaleAtNatOriginGeoKey, getK(1.0))
    encoder.todoGeoKeyDouble(ProjFalseEastingGeoKey, getDouble("x_0"))
    encoder.todoGeoKeyDouble(ProjFalseNorthingGeoKey, getDouble("y_0"))
  }

  def setUTMProps(encoder: Encoder) {
    val pcKey = reversedProjMethodToCTProjMethodMap(CT_TransverseMercator)
    val zone = getInt("zone")
    val south = proj4Map.contains("south")

    encoder.todoGeoKeyInt(GTModelTypeGeoKey, ModelTypeProjected)
    encoder.todoGeoKeyInt(ProjectedCSTypeGeoKey, UserDefinedCPV)
    encoder.todoGeoKeyInt(ProjCoordTransGeoKey, pcKey)
    encoder.todoGeoKeyDouble(ProjNatOriginLatGeoKey, 0.0)
    encoder.todoGeoKeyDouble(ProjNatOriginLongGeoKey, zone * 6 - 183)
    encoder.todoGeoKeyDouble(ProjScaleAtNatOriginGeoKey, 0.9996)
    encoder.todoGeoKeyDouble(ProjFalseEastingGeoKey, 500000.0)
    encoder.todoGeoKeyDouble(
      ProjFalseNorthingGeoKey, if (south) 0.0 else 10000000.0
    )
  }

  def setLCCProps(encoder: Encoder) {
    val pcKey = reversedProjMethodToCTProjMethodMap(CT_LambertConfConic_1SP)
    val lat0 = getDouble("lat_0")
    val lat1 = getDouble("lat_1")

    encoder.todoGeoKeyInt(GTModelTypeGeoKey, ModelTypeProjected)
    encoder.todoGeoKeyInt(ProjectedCSTypeGeoKey, UserDefinedCPV)
    encoder.todoGeoKeyInt(ProjectionGeoKey, UserDefinedCPV)
    encoder.todoGeoKeyInt(ProjCoordTransGeoKey, pcKey)
    encoder.todoGeoKeyDouble(ProjNatOriginLatGeoKey, lat0)
    encoder.todoGeoKeyDouble(ProjNatOriginLongGeoKey, getDouble("lon_0"))

    if (lat0 == lat1) {
      encoder.todoGeoKeyDouble(ProjScaleAtNatOriginGeoKey, getK(1.0))
    } else {
      val lat2 = getDouble("lat_2")
      encoder.todoGeoKeyDouble(ProjStdParallel1GeoKey, lat1)
      encoder.todoGeoKeyDouble(ProjStdParallel2GeoKey, lat2)
    }

    encoder.todoGeoKeyDouble(ProjFalseEastingGeoKey, getDouble("x_0"))
    encoder.todoGeoKeyDouble(ProjFalseNorthingGeoKey, getDouble("y_0"))
  }

  def setGCSOrDatum(encoder: Encoder) =
    if (gcs != UserDefinedCPV) encoder.todoGeoKeyInt(GeogTypeGeoKey, gcs)
    else if (datum != UserDefinedCPV) {
      encoder.todoGeoKeyInt(GeogTypeGeoKey, UserDefinedCPV)
      encoder.todoGeoKeyInt(GeogGeodeticDatumGeoKey, datum)
    } else throw new MalformedProj4Exception(
      s"Neither datum or GCS specified for proj4 string $proj4String"
    )

  def setEllipsoid(encoder: Encoder) = if (gcs == UserDefinedCPV) {
    encoder.todoGeoKeyInt(GeogEllipsoidGeoKey, ellipsoid)

    if (!optSemis.isEmpty) {
      val (major, minor, invFlattening) = optSemis.get
      encoder.todoGeoKeyDouble(GeogSemiMajorAxisGeoKey, major)
      encoder.todoGeoKeyDouble(GeogSemiMinorAxisGeoKey, minor)
      encoder.todoGeoKeyDouble(GeogInvFlatteningGeoKey, invFlattening)
    }
  }

  def setLinearUnits(encoder: Encoder) = {
    val unitString = getString("units", "err")
    val code = reversedProjectedLinearUnitsMap.getOrElse(unitString, UserDefinedCPV)
    encoder.todoGeoKeyInt(ProjLinearUnitsGeoKey, code)

    if (code == UserDefinedCPV) {
      val toMeters = getDouble("to_meter", "-1")
      if (toMeters != -1) encoder.todoGeoKeyDouble(
        ProjLinearUnitSizeGeoKey,
        toMeters
      )
    }
  }

  private def getString(key: String, defV: String) =
    proj4Map.getOrElse(key, defV)

  private def getInt(key: String, defV: String = "0") =
    proj4Map.getOrElse(key, defV).toInt

  private def getDouble(key: String, defV: String = "0.0") =
    getString(key, defV).toDouble

  def getK(defV: Double) = proj4Map.get("k") match {
    case Some(k) => k.toDouble
    case None => proj4Map.get("k_0") match {
      case Some(k) => k.toDouble
      case None => defV
    }
  }

}
