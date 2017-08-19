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

package geotrellis.proj4

import org.scalatest._

/**
 * @author Manuri Perera
 */
class CRSTest extends FunSpec with Inspectors {

  it("should return the proj4string corresponding to EPSG:4326") {
    val crs = CRS.fromName("EPSG:4326")

    val proj4string = crs.toProj4String
    val string = "+proj=longlat +datum=WGS84 +no_defs "

    assert(proj4string == string)
  }

  it("should return the proj4string corresponding to the EPSG:3857") {
    val crs = CRS.fromName("EPSG:3857")

    val proj4string = crs.toProj4String
    val string = "+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext +no_defs "

    assert(proj4string == string)
  }

  it("should return the WKT string of the passed EPSG:4326") {
    val crs = CRS.fromName("EPSG:4326")

    val wktString = crs.toWKT()

    assert(wktString == Some("GEOGCS[\"WGS 84\", DATUM[\"World Geodetic System 1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4326\"]]"))

  }

  it("should return the WKT string of the passed EPSG:4010") {
    val crs = CRS.fromName("EPSG:4010")

    val wktString = crs.toWKT()

    assert(wktString == Some("GEOGCS[\"Unknown datum based upon the Clarke 1880 (Benoit) ellipsoid\", DATUM[\"Not specified (based on Clarke 1880 (Benoit) ellipsoid)\", SPHEROID[\"Clarke 1880 (Benoit)\", 6378300.789, 293.4663155389811, AUTHORITY[\"EPSG\",\"7010\"]], AUTHORITY[\"EPSG\",\"6010\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4010\"]]"))
  }

  it("should return the WKT string of the passed EPSG:3857") {
    val crs = CRS.fromName("EPSG:3857")

    val wktString = crs.toWKT()

    assert(wktString == Some("PROJCS[\"WGS 84 / Pseudo-Mercator\", GEOGCS[\"WGS 84\", DATUM[\"World Geodetic System 1984\", SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], AUTHORITY[\"EPSG\",\"6326\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4326\"]], PROJECTION[\"Popular Visualisation Pseudo Mercator\", AUTHORITY[\"EPSG\",\"1024\"]], PARAMETER[\"semi_minor\", 6378137.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"central_meridian\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 0.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"3857\"]]"))
  }

  it("should have human friendly toString") {
    val samples = Seq(
      CRS.fromEpsgCode(3857),
      CRS.fromWKT(WebMercator.toWKT().get),
      CRS.fromName("EPSG:4326"),
      CRS.fromString(Sinusoidal.toProj4String),
      LatLng, Sinusoidal, WebMercator, ConusAlbers
    )

    forEvery(samples) { crs ⇒
      val str = crs.toString
      assert(!str.contains("$") && !str.contains("@"))
    }
  }
}
