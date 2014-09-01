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

package geotrellis.proj4.datum

import geotrellis.proj4.ProjCoordinate

object Datum {

  object Constants {
    val TYPE_UNKNOWN = 0
    val TYPE_WGS84 = 1
    val TYPE_3PARAM = 2
    val TYPE_7PARAM = 3
    val TYPE_GRIDSHIFT = 4

    val DEFAULT_TRANSFORM = Vector(0.0, 0.0, 0.0)

    val ELLIPSOID_E2_TOLERANCE = 0.000000000050
  }

  lazy val WGS84 = Datum("WGS84", 0,0,0, Ellipsoid.WGS84, "WGS84")
  lazy val GGRS87 = Datum("GGRS87", -199.87,74.79,246.62, Ellipsoid.GRS80, "Greek_Geodetic_Reference_System_1987")
  lazy val NAD83 = Datum("NAD83", 0,0,0, Ellipsoid.GRS80,"North_American_Datum_1983")
  lazy val NAD27 = Datum("NAD27", "@conus,@alaska,@ntv2_0.gsb,@ntv1_can.dat", Ellipsoid.CLARKE_1866,"North_American_Datum_1927") //TODO: Handle transform specification
  lazy val POTSDAM = Datum("potsdam", 606.0,23.0,413.0, Ellipsoid.BESSEL, "Potsdam Rauenberg 1950 DHDN")
  lazy val CARTHAGE = Datum("carthage",-263.0,6.0,431.0, Ellipsoid.CLARKE_1880, "Carthage 1934 Tunisia")
  lazy val HERMANNSKOGEL = Datum("hermannskogel", 653.0,-212.0,449.0, Ellipsoid.BESSEL, "Hermannskogel")
  lazy val IRE65 = Datum("ire65", 482.530,-130.596,564.557,-1.042,-0.214,-0.631,8.15, Ellipsoid.MOD_AIRY, "Ireland 1965")
  lazy val NZGD49 = Datum("nzgd49", 59.47,-5.04,187.44,0.47,-0.1,1.024,-4.5993, Ellipsoid.INTERNATIONAL, "Zealand Geodetic Datum 1949")
  lazy val OSEB36 = Datum("OSGB36", 446.448,-125.157,542.060,0.1502,0.2470,0.8421,-20.4894, Ellipsoid.AIRY, "Airy 1830")

  def apply(code: String, transformSpec: String, ellipsoid: Ellipsoid, name:String): Datum =
    // TODO: implement handling of transform specification
    apply(code, ellipsoid, name, Constants.DEFAULT_TRANSFORM)

  def apply(
    code: String,
    ellipsoid: Ellipsoid,
    name: String,
    transform: Array[Double]
  ): Datum = 
    apply(code, ellipsoid, name, transform.toVector)

  def apply(code: String, deltaX: Double, deltaY: Double, deltaZ: Double, ellipsoid: Ellipsoid, name: String): Datum =
    apply(code, ellipsoid, name, Array(deltaX, deltaY, deltaZ))
  
  def apply(code: String, deltaX: Double, deltaY: Double, deltaZ: Double, rx: Double, ry: Double, rz: Double, mbf: Double,
    ellipsoid: Ellipsoid,
    name: String
  ): Datum =
    apply(code, ellipsoid, name, Array(deltaX, deltaY, deltaZ, rx, ry, rz, mbf))
}

import Datum.Constants._

case class Datum(
  code: String,
  ellipsoid: Ellipsoid,
  name: String,
  transform: Vector[Double] = Datum.Constants.DEFAULT_TRANSFORM) {

  override def toString: String = s"[Datum-$name]"

  lazy val transformType: Int =
    if (isIdentity(transform)) TYPE_WGS84
    else if (transform.length == 3) TYPE_3PARAM
    else if (transform.length == 7) TYPE_7PARAM
    else TYPE_WGS84

  private def isIdentity(transform: Vector[Double]) =
    if (transform.size >= 7 && transform(6) != 1.0 && transform(6) != 0.0) false
    else (transform.slice(0, 6) ++ transform.drop(7)).filter(_ != 0.0).size == 0

  // TODO: Can this really be false? Doesn't seem like Proj4J handled the case where it was.
  lazy val hasTransformToWGS84: Boolean =
    transformType == TYPE_3PARAM || transformType == TYPE_7PARAM

  override def equals(that: Any) = that match {
    case datum: Datum =>
      if (datum.transformType != transformType) false
      else if (ellipsoid.equatorRadius != datum.ellipsoid.equatorRadius
        && math.abs(ellipsoid.eccentricitySquared - datum.ellipsoid.eccentricitySquared)
        > ELLIPSOID_E2_TOLERANCE) false
      else if ((transformType == TYPE_3PARAM || transformType == TYPE_7PARAM)
        && transform != datum.transform) false
      else true
    case _ => false
  }

  def transformFromGeocentricToWgs84(projCoordinate: ProjCoordinate): ProjCoordinate =
    if (transform.length == 3) {
      ProjCoordinate(
        projCoordinate.x + transform(0),
        projCoordinate.y + transform(1),
        projCoordinate.z + transform(2)
      ) 
    } else if (transform.length == 7) {
      val dx = transform(0)
      val dy = transform(1)
      val dz = transform(2)
      val rx = transform(3)
      val ry = transform(4)
      val rz = transform(5)
      val m  = transform(6)

      val xOut =
        m * (projCoordinate.x - rz * projCoordinate.y + ry * projCoordinate.z) + dx
      val yOut =
        m * (rz * projCoordinate.x + projCoordinate.y - rx * projCoordinate.z) + dy
      val zOut =
        m * (-ry * projCoordinate.x + rx * projCoordinate.y + projCoordinate.z) + dz

      ProjCoordinate(xOut, yOut, zOut)
    } else {
      projCoordinate
    }

  def transformToGeocentricFromWgs84(projCoordinate: ProjCoordinate): ProjCoordinate =
    if (transform.length == 3) {
      ProjCoordinate(
        projCoordinate.x - transform(0),
        projCoordinate.y - transform(1),
        projCoordinate.z - transform(2)
      ) 
    } else if (transform.length == 7) {
      val dx = transform(0)
      val dy = transform(1)
      val dz = transform(2)
      val rx = transform(3)
      val ry = transform(4)
      val rz = transform(5)
      val m  = transform(6)

      val xTmp = (projCoordinate.x - dx) / m
      val yTmp = (projCoordinate.y - dy) / m
      val zTmp = (projCoordinate.z - dz) / m

      val xOut = xTmp + rz * yTmp - ry * zTmp
      val yOut = -rz * xTmp + yTmp + rx * zTmp
      val zOut = -ry * xTmp - rx * yTmp + zTmp

      ProjCoordinate(xOut, yOut, zOut)
    } else {
      projCoordinate
    }
}
