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

  val TYPE_UNKNOWN = 0
  val TYPE_WGS84 = 1
  val TYPE_3PARAM = 2
  val TYPE_7PARAM = 3
  val TYPE_GRIDSHIFT = 4

  val DEFAULT_TRANSFORM = Vector(0.0, 0.0, 0.0)

  val ELLIPSOID_E2_TOLERANCE = 0.000000000050
}

case class Datum(
  code: String,
  ellipsoid: Ellipsoid,
  name: String,
  transform: Vector[Double] = DEFAULT_TRANSFORM) {

  override def toString: String = s"[Datum-$name]"

  lazy val transformType: Int =
    if (isIdentity(transform)) TYPE_WGS84
    else if (transform.length == 3) TYPE_3PARAM
    else if (transform.length == 7) TYPE_7PARAM
    else TYPE_WGS84

  private def isIdentity(transform: Vector[Double]) =
    if (transform.size >= 7 && transform(6) != 1.0 && transform(6) != 0.0) false
    else (transform.slice(0, 6) ++ transform.drop(7)).filter(_ != 0.0).size == 0

  lazy val hasTransformToWGS84: Boolean =
    transformType == TYPE_3PARAM || transformType == TYPE_7PARAM

  override def equals(that: Any) = that match {
    case datum: Datum =>
      if (datum.transformType != tranformType) false
      else if (ellipsoid.equatorRadius != datum.ellipsoid.equatorRadius
        && math.abs(ellipsoid.eccentricitySquared - datum.ellipsoid.eccentricitySquared)
        > ELLIPSOID_E2_TOLERANCE) false
      else if ((transformType == TYPE_3PARAM || tranformType == TYPE_7PARAM)
        && transform != datum.transform) false
      else true
    case _ => false
  }

  def transformFromGeocentricToWgs84(projCoordinate: ProjCoordinate): ProjCoordinate =
    if (transform.length == 3) ProjCoordinate(
      projCoordinate.x += transform(0),
      projCoordinate.y += transform(1),
      projCoordinate.z += transform(2)
    ) else if (transform.length == 7) {
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
    } else projCoordinate

  def transformToGeocentricFromWgs84(projCoordinate: ProjCoordinate): ProjCoordinate =
    if (transform.length == 3) ProjCoordinate(
      projCoordinate.x -= transform(0),
      projCoordinate.y -= transform(1),
      projCoordinate.z -= transform(2)
    ) else if (transform.length == 7) {
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
    } else projCoordinate
}
