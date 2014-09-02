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

object Proj4StringParser {

  def apply(proj4Map: Map[String, String]): Proj4StringParser =
    new Proj4StringParser(proj4Map)

}

class Proj4StringParser(proj4String: String) {

  import Proj4StringParser._

  val proj4Map: Map[String, String] =
    proj4String.split('+').
      map(_.trim).
      filter(_.contains('=')).
      groupBy(s => s.takeWhile(_ != '=')).
      map( case (a, b) => (a, b.head) )

  lazy val parseEllipsoid: (Int, Option[(Double, Double, Double)]) =
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

  lazy val parseDatum: (Int, Int) = proj4Map.get("datum") match {
    case Some("WGS84") => (GCS_WGS_84, Datum_WGS84)
    case Some("NAD83") => (GCS_NAD83, Datum_North_American_Datum_1983)
    case Some("NAD27") => (GCS_NAD27, Datum_North_American_Datum_1927)
    case _ => (UserDefinedCPV, UserDefinedCPV)
  }

  def parseProjection(encoder: Encoder): Int = proj4Map.get("proj") match {
    case Some("tmerc") => setTMercProps(encoder)
    case Some("longlat") | Some("latlong") => 0
    case _ => throw new illegalStateException(
      "Proj4 Strin doesn't have a valid proj flag or is missing one."
    )
  }

  def setTMercProps(encoder: Encoder): Int = {
    encoder.todoGeoKey(GTModelTypeGeoKey, ModelTypeProjected)
    encoder.todoGeoKey(ProjectedCSTypeGeoKey, UserDefinedCPV)
    encoder.todoGeoKey(ProjCoordTransGeoKey, CT_TransverseMercator)
    encoder.todoGeoKey(ProjNatOriginLatGeoKey, getDouble("lat_0"))
    encoder.todoGeoKey(ProjNatOriginLongGeoKey, getDouble("lon_0"))
    encoder.todoGeoKey(ProjScaleAtNatOriginGeoKey, getDouble("k"))
    encoder.todoGeoKey(ProjFalseEastingGeoKey, getDouble("x_0"))
    encoder.todoGeoKey(ProjFalseNorthingGeoKey, getDouble("y_0"))

    8
  }

  private def getString(key: String, defV: String) =
    proj4Map.getOrElse(key, defV)

  private def getDouble(key: String, defV: String = "0.0") =
    getString(key, defV).toDouble

}
