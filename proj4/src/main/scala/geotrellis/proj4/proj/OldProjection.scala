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

// package geotrellis.proj4

// import geotrellis.proj4.util.ProjectionMath._

// object Projection {

//   def normalizeLongitude(angle: Double): Double =
//     if (angle.isInfinite || angle.isNaN)
//       throw new InvalidValueException("Infinite or NaN longitude.")
//     else {
//       var a = angle

//       while (a > 180) a -= 360
//       while (a < -180) a += 360

//       a
//     }

//   def normalizeLongitudeRadians(angle: Double): Double =
//     if (angle.isInfinite || angle.isNaN)
//       throw new InvalidValueException("Infinite or NaN longitude.")
//     else {
//       var a = angle

//       while (a > math.Pi) a -= TWO_PI
//       while (a < -math.Pi) a += TWO_PI

//       a
//     }

// }

// abstract class Projection(
//   protected val minLatitude = -math.Pi / 2,
//   protected val minLongitude = -math.Pi,
//   protected val maxLatitude = math.Pi / 2,
//   protected val maxLongitude = math.Pi,
//   protected val projectionLatitude = 0.0,
//   protected val projectionLongitude = 0.0,
//   protected val projectionLatitude1 = 0.0,
//   protected val projectionLatitude2 = 0.0,
//   protected val alpha = Double.NaN,
//   protected val lonc = Double.NaN,
//   protected val scaleFactor = 1.0,
//   protected val falseEasting = 0,
//   protected val falseNorthing = 0,
//   protected val isSouth = false,
//   protected val trueScaleLatitude = 0.0,
//   protected val a = 0,
//   protected val e = 0,
//   protected val es = 0,
//   protected val one_es = 0,
//   protected val rone_es = 0,
//   protected ellipsoid = Ellipsoid.SPHERE,
//   protected val spherical = false,
//   protected val geocentric = false,
//   protected val name: Option[String] = None,
//   protected val fromMetres = 1,
//   protected val totalScale = 0,
//   private val totalFalseEasting = 0,
//   private val totalFalseNorthing = 0,
//   protected val unit: Option[Unit] = None,
// ) {

//   def project(source: ProjCoordinate, dest: ProjCoordinate): ProjCoordinate = {
//     val x =
//       if (projectionLongitude != 0)
//         normalizeLongitude(source.x * DTR - projectionLongitude)
//       else source.x * DTR

//     projectRadians(x, soruce.y * DTR, dest)
//   }

//   def projectRadians(source: ProjCoordinate: dest: ProjCoordinate): ProjCoordinate =
//     project(source, dest)

//   def projectRadians(x: Double, y: Double, dest: ProjCoordinate): ProjCoordinate = {
//     val pDest = project(x, y, dest)

//     if (unit == Units.DEGREES)
//       ProjCoordinate(pDest.x * RTD, pDest.y * RTD, pDest.z)
//     else
//       ProjCoordinate(
//         totalScale * dest.x + totalFalseEasting,
//         totalScale * dest.y + totalFalseNorthing,
//         pDest.z)
//   }

//   protected def project(
//     x: Double,
//     y: Double,
//     dest: ProjCoordinate): ProjCoordinate = ProjCoordinate(x, y, dest.z)

//   def inverseProject(source: ProjCoordinate, dest: ProjCoordinate): ProjCoordinate = {
//     val pDest = inverseProjectRadians(source, dest)
//     ProjCoordinate(pDest.x * RTD, pDest.y * RTD, pDest.z)
//   }

//   def inverseProjectRadians(
//     source: ProjCoordinate,
//     dest: ProjCoordinate): ProjCoordinate = {
//     val (x, y) =
//       if (unit == Units.DEGREES)
//         (source.x * DTR, source.y * DTR)
//       else
//         (
//           (source.x - totalFalseEasting) / totalScale,
//           (source.y - totalFalseNorthing) / totalScale
//         )

//     val pDest = projectInverse(x, y, dest)
//     val dstX =
//       if (pDest.x < - math.Pi) -math.Pi
//       else if (pDest.x > math.Pi) math.Pi
//       else pDest.x

//     ProjCoordinate(
//       (if (projectionLongitude != 0) normalizeLongitude(dstX + projectionLongitude)
//       else dstX),
//       pDest.y
//     )
//   }

//   def projectInverse(x: Double, y: Double, dest: ProjCoordinate)

//   def isConformal: Boolean = false

//   def isEqualArea: Boolean = false

//   def hasInverse: Boolean = false

//   def isRectilinear: Boolean = false

//   def parallelsAreParallel: Boolean = isRectilinear

//   def inside(x: Double, y: Double): Boolean = {
//     val xn = normalizeLongitude(x * DTR - projectionLongitude)
//     minLongitude <= xn && xn <= maxLongitude && minLatitude <= y && y <= maxLatitude
//   }

//   def setName(name: String): Projection =

// }
