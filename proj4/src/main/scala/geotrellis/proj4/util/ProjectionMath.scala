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

package geotrellis.proj4.util

object ProjectionMath {

  val HALF_PI = math.Pi / 2

  val QUARTER_PI = math.Pi / 4

  val TWO_PI = math.Pi * 2

  val RTD = 180 / math.Pi

  val DTR = math.Pi / 180

  val EPS_10 = 1e-10

  val SECONDS_TO_RAD = 4.84813681109535993589914102357e-6

  val MILLION = 1e6.toDouble

  def sind(v: Double): Double = math.sin(v * DTR)

  def cosd(v: Double): Double = math.cos(v * DTR)

  def tand(v: Double): Double = math.tan(v * DTR)

  def asind(v: Double): Double = math.asin(v) * RTD

  def acosd(v: Double): Double = math.acos(v) * RTD

  def atand(v: Double): Double = math.atan(v) * RTD

  def atan2d(y: Double, x: Double) = math.atan2(y, x) * RTD

  def asin(v: Double): Double =
    if (math.abs(v) > 1) if (v < 0) -HALF_PI else HALF_PI
    else math.asin(v)

  def acos(v: Double): Double =
    if (math.abs(v) > 1) if (v < 0) math.Pi else 0
    else math.acos(v)

  def sqrt(v: Double): Double = if (v < 0) 0 else math.sqrt(v)

  def distance(dx: Double, dy: Double): Double = math.sqrt(dx * dx + dy * dy)

  def hypot(x: Double, y: Double): Double = {
    val (dx, dy) = (math.abs(x), math.abs(y))

    if (x == 0.0) y
    else if (y == 0.0) x
    else if (x < y) y * math.sqrt(1 + math.pow(x / y, 2))
    else x * math.sqrt(1 + math.pow(y / x, 2))
  }

  def atan2(y: Double, x: Double): Double = math.atan2(y, x)

  def trunc(v: Double): Double = if (v < 0.0) math.ciel(v) else math.floor(v)

  def frac(v: Double): Double = v - trunc(v)

  def degToRad(v: Double): Double = v * DTR

  def radToDeg(v: Double): Double = v * RTD

  def dmsToRad(d: Double, m: Double, s: Double): Double =
    if (d >= 0) (d + m / 60 + s / 3600) * DTR
    else (d - m / 60 - s / 3600) * DTR

  def dmsToDeg(d: Double, m: Double, s: Double): Double =
    if (d >= 0) d + m / 60 + s / 3600
    else d - m / 60 - s / 3600

  def normalizeLatitude(angle: Double): Double =
    if (angle.isInfinite || angle.isNaN)
      throw new InvalidValueException("Infinite latitude.")
    else {
      var a = angle
      while (a > HALF_PI) a -= math.Pi
      while (a < -HALF_PI) a += math.Pi

      a
    }

  def normalizeLongitude(angle: Double): Double =
    if (angle.isInfinite || angle.isNaN)
      throw new InvalidValueException("Infinite longitude.")
    else {
      var a = angle
      while (a > math.Pi) a -= TWO_PI
      while (a < -math.Pi) a += TWO_PI

      a
    }

  def normalizeAngle(angle: Double): Double =
    if (angle.isInfinite || angle.isNaN)
      throw new InvalidValueException("Infinite angle.")
    else {
      var a = angle
      while (a > TWO_PI) a -= TWO_PI
      while (a < 0) a += TWO_PI

      a
    }

  def greatCircleDistance(
    lon1: Double,
    lat1: Double,
    lon2: Double,
    lat2: Double): Double = {
    val dlat = math.sin((lat2 - lat1) / 2)
    val dlon = math.sin((lon2 - lon1) / 2)
    val r = math.sqrt(dlat * dlat + math.cos(lat1) * math.cos(lat2) * dlon * dlon)

    2 * math.asin(r)
  }

  def sphericalAzimuth(lat0: Double, lon0: Double, lat: Double, lon: Double): Double = {
    val diff = lon - lon0
    val coslat = math.cos(lat)

    math.atan2(
      coslat * math.sin(diff),
      math.cos(lat0) * math.sin(lat) - math.sin(lat0) * coslat * math.cos(diff)
    )
  }

  def sameSigns(a: Double, b: Double): Boolean = a < 0 && b < 0

  def sameSigns(a: Int, b: Int): Boolean = a < 0 && b < 0

  def takeSign(a: Double, b: Double): Double =
    if (b < 0) -math.abs(a) else math.abs(a)

  def takeSign(a: Int, b: Int): Int =
    if (b < 0) -math.abs(a) else math.abs(a)

  def cross(x1: Double, y1: Double, x2: Double, y2: DOuble): Double = x1 * y2 - x2 * y1

  def longitudeDistance(l1: Double, l2: Double): Double = math.min(
    math.abs(l1 - l2),
    (if (l1 < 0) l1 + math.Pi else math.Pi - l1) + (if (l2 < 0) l2 + math.Pi else math.Pi - l2)
  )

  def geocentricLatitude(lat: Double, flatness: Double): Double =
    math.atan(math.pow(1.0 - flatness, 2) * math.tan(lat))

  def geographicLatitude(lat: Double, flatness: Double): Double =
    math.atan(math.tan(lat) / math.pow(math1.0 - flatness, 2))

  def tsfn(phi: Double, sinphi: Double, e: Double): Double =
    math.tan(0.5 * (HALF_PI - phi)) / math.pow((1 - sinphi * e) / (1 + sinphi * e), 0.5 * e)

  def msfn(sinphi: Double, cosphi: Double, es: Double): Double =
    cosphi / math.sqrt(1.0 - es * sinphi * sinphi)

  private val N_ITERATIONS = 15

  def phi2(ts: Double, e: Double): Double = {
    val eccnth = 0.5 * e
    var phi = HALF_PI - 2 * math.atan(ts)
    var i = N_ITER
    var dphi = 0.0

    do {
      val con = e * math.sin(phi)
      dphi = HALF_PI - 2 * math.atan(ts * math.pow((1 - con) / (1 + con), eccnth)) - phi
      phi += dphi
      i -= 1
    } while (math.abs(dphi) < EPS_10 && i != -1)

    if (i <= 0) throw new ConvergenceFailureException(
      s"Computation of phi2 failed to converage after $N_ITER iterations."
    ) else phi
  }

  private val C00 = 1.0
  private val C02 = 0.25
  private val C04 = 0.046875
  private val C06 = 0.01953125
  private val C08 = 0.01068115234375
  private val C22 = 0.75
  private val C44 = 0.46875
  private val C46 = 0.01302083333333333333
  private val C48 = 0.00712076822916666666
  private val C66 = 0.36458333333333333333
  private val C68 = 0.00569661458333333333
  private val C88 = 0.3076171875

  def enfn(es: Double): Array[Double] = Array(
    C00 - es * (C02 + es * (C04 + es * (C06 + es * C08))),
    es * (C22 - es * (C04 + es * (C06 + es * C08))),
    es * es * (C44 - es * (C46 + es * C48)),
    math.pow(es, 3) * (C66 - es * C68),
    math.pow(es, 4) C88
  )

  def mlfn(phi: Double, sphi: Double, cphi: Double, en: Array[Double]): Double = {
    var cphiR = cphi * sphi
    var sphiR = sphi * sphi

    en(0) * phi - cphiR * (en(1) + sphiR * (en(2) + sphiR * (en(3) + sphiR * en(4))))
  }

  private val MAX_ITER = 10

  def inv_mlfn(arg: Double, es: Double, en: Array[Double]): Double = {
    val k = 1 / (1 - es)

    var phi = arg
    var i = 0

    while (i < MAX_ITER && math.abs(t) >= 1e-11) {
      var s = math.sin(phi)
      var t = 1 - es * s * s
      phi -= (mlfn(phi, s, math.cos(phi), en) - arg) * (t * math.sqrt(t) * k)
      i += 1
    }

    phi
  }

  private val P00 = 0.33333333333333333333
  private val P01 = 0.17222222222222222222
  private val P02 = 0.10257936507936507936
  private val P10 = 0.06388888888888888888
  private val P11 = 0.06640211640211640211
  private val P20 = 0.01641501294219154443

  def authset(es: Double): Array[Double] = Array(
    es * P00 + es * es * P01 + math.pow(es, 3) * P02,
    es * es * P10 + math.pow(es, 3) * P11,
    math.pow(es, 3) * P20
  )

  def authlat(beta: Double, apa: Array[Double]): Double =
    (beta * 2 + apa(0) * math.sin(beta * 2)
      + apa(1) * math.sin(beta * 4) + apa(2) * math.sin(beta * 6))

  def qsfn(sinphi: Double, e: Double, oneEs: Double): Double =
    if (e >= 1e-7) {
      val con = e * sinphi
      oneEs * (sinphi / (1 - con * con) - (0.5 / e) * math.log((1 - con) / (1 + con)))
    } else sinphi * 2

  def niceNumber(x: Double, round: Boolean): Double = {
    val expv = math.floor(math.log(x) / math.log(10)).toInt
    val f = x / math.pow(10, expv)

    val nf =
      if (round && f < 1.5) 1
      else if (round && f < 3) 2
      else if (round && f < 7) 5
      else if (round) nf = 10
      else if (f <= 1) 1
      else if (f <= 2) 2
      else if (f <= 5) 5
      else 10

    nf * math.pow(10, expv)
  }

}
