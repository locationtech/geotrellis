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

package geotrellis.proj4.proj

import geotrellis.proj4.util.ProjectionMath._
import geotrellis.proj4.units._
import geotrellis.proj4._

object Projection {

  def normalizeLongitude(angle: Double): Double =
    if (angle.isInfinite || angle.isNaN)
      throw new InvalidValueException("Infinite or NaN longitude.")
    else {
      var a = angle

      while (a > 180) a -= 360
      while (a < -180) a += 360

      a
    }

  def normalizeLongitudeRadians(angle: Double): Double =
    if (angle.isInfinite || angle.isNaN)
      throw new InvalidValueException("Infinite or NaN longitude.")
    else {
      var a = angle

      while (a > math.Pi) a -= TWO_PI
      while (a < -math.Pi) a += TWO_PI

      a
    }

}

abstract class Projection(
  val name: String,
  val minLatitude: Double,
  val minLongitude: Double,
  val maxLatitude: Double,
  val maxLongitude: Double,
  val projectionLatitude: Double,
  val projectionLongitude: Double,
  val projectionLatitude1: Double,
  val projectionLatitude2: Double,
  val trueScaleLatitude: Double,
  val alpha: Double,
  val lonc: Double,
  val scaleFactor: Double,
  val falseEasting: Double,
  val falseNorthing: Double,
  val isSouth: Boolean,
  val ellipsoid: Ellipsoid,
  val fromMetres: Double,
  val unitOption: Option[Unit],
  val zoneOption: Option[Int]
) {

  val a: Double = ellipsoid.equatorRadius

  val e: Double = ellipsoid.eccentricity

  val es: Double = ellipsoid.eccentricity2

  val spherical: Boolean = e == 0.0

  val oneEs: Double = 1 - es

  val rOneEs: Double = 1.0 / oneEs

  val totalScale: Double = a * fromMetres

  val totalFalseEasting: Double = falseEasting * fromMetres

  val totalFalseNorthing: Double = falseNorthing * fromMetres

  def project(source: ProjCoordinate, dest: ProjCoordinate): ProjCoordinate = {
    val x =
      if (projectionLongitude != 0)
        normalizeLongitude(source.x * DTR - projectionLongitude)
      else source.x * DTR

    projectRadians(x, source.y * DTR, dest)
  }

  def projectRadians(source: ProjCoordinate, dest: ProjCoordinate): ProjCoordinate =
    project(source, dest)

  def projectRadians(x: Double, y: Double, dest: ProjCoordinate): ProjCoordinate = {
    val pDest = project(x, y, dest)

    unitOption match {
      case Some(Units.DEGREES) =>
        ProjCoordinate(pDest.x * RTD, pDest.y * RTD, pDest.z)
      case _ =>
        ProjCoordinate(
          totalScale * dest.x + totalFalseEasting,
          totalScale * dest.y + totalFalseNorthing,
          pDest.z)
    }
  }

  protected def project(
    x: Double,
    y: Double,
    dest: ProjCoordinate): ProjCoordinate = ProjCoordinate(x, y, dest.z)

  def inverseProject(source: ProjCoordinate, dest: ProjCoordinate): ProjCoordinate = {
    val pDest = inverseProjectRadians(source, dest)
    ProjCoordinate(pDest.x * RTD, pDest.y * RTD, pDest.z)
  }

  def inverseProjectRadians(
    source: ProjCoordinate,
    dest: ProjCoordinate): ProjCoordinate = {
    val (x, y) = unitOption match {
      case Some(Units.DEGREES) => (source.x * DTR, source.y * DTR)
      case _ => (
        (source.x - totalFalseEasting) / totalScale,
        (source.y - totalFalseNorthing) / totalScale
      )
    }

    val pDest = projectInverse(x, y, dest)
    val dstX =
      if (pDest.x < - math.Pi) -math.Pi
      else if (pDest.x > math.Pi) math.Pi
      else pDest.x

    ProjCoordinate(
      (if (projectionLongitude != 0) normalizeLongitude(dstX + projectionLongitude)
      else dstX),
      pDest.y
    )
  }

  def projectInverse(x: Double, y: Double, dest: ProjCoordinate): ProjCoordinate

  def isConformal: Boolean = false

  def isEqualArea: Boolean = false

  def hasInverse: Boolean = false

  def isRectilinear: Boolean = false

  def parallelsAreParallel: Boolean = isRectilinear

  def inside(x: Double, y: Double): Boolean = {
    val xn = normalizeLongitude(x * DTR - projectionLongitude)
    minLongitude <= xn && xn <= maxLongitude && minLatitude <= y && y <= maxLatitude
  }

  def getPROJ4Description: String = {
    val format = new AngleFormat(AngleFormat.ddmmssPattern, false)
    val sb = new StringBuffer()

    sb.append(s"+proj=${name} +a=$a")

    if (es != 0) sb.append(s" +es=$es")

    sb.append(" +lon_0=")
    format.format(projectionLongitude, sb)

    sb.append(" +lat_0=")
    format.format(projectionLatitude, sb)

    if (falseEasting != 1) sb.append(s" +x_0=$falseEasting")

    if (falseNorthing != 1) sb.append(s" +y_0=$falseNorthing")

    if (scaleFactor != 1) sb.append(s" +k=$scaleFactor")

    if (fromMetres != 1) sb.append(s" +fr_meters=$fromMetres")

    sb.toString
  }

  override def toString: String = "None"

}
