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

package geotrellis.proj4.parser

import geotrellis.proj4.datum._

object DatumParameters {

  val SIXTH = 0.1666666666666666667
  val RA4 = 0.04722222222222222222
  val RA6 = 0.02215608465608465608
  val RV4 = 0.06944444444444444444
  val RV6 = 0.04243827160493827160

}

case class DatumParameters(
  optDatum: Option[Datum],
  datumTransform: Array[Double],
  optEllipsoid: Option[Ellipsoid],
  a: Double = Double.NaN,
  es: Double = Double.NaN) {

  import DatumParameters._

  def this() = this(None, Array[Double](), None)

  lazy val datum = optDatum match {
    case Some(d) => d
    case None => optEllipsoid match {
      case Some(e) if (e == Ellipsoid.WGS84) => Datum.WGS84
      case None if (!isDefinedExplicitly) => Datum.WGS84
      case _ => Datum("User", datumTransform, ellipsoid, "User-defined")
    }
  }

  lazy val isDefinedExplicitly: Boolean = !a.isNaN && !es.isNaN

  lazy val ellipsoid: Ellipsoid =
    optEllipsoid getOrElse Ellipsoid("user", a, es, "User-defined")

  def setDatumTransform(datumTransform: Array[Double]): DatumParameters =
    DatumParameters(optDatum, datumTransform, optEllipsoid, a, e)

  def setDatum(datum: Datum): DatumParameters =
    DatumParameters(Some(datum), datumTransform, optEllipsoid, a, e)

  def setEllipsoid(ellipsoid: Ellipsoid): DatumParameters =
    DatumParameters(optDatum,
      datumTransform,
      Some(ellipsoid),
      ellipsoid.eccentricity2,
      ellipsoid.equatorRadius
    )

  def setA(a: Double): DatumParameters =
    DatumParameters(optDatum, datumTransform, None, a, es)

  def setB(b: Double): DatumParameters =
    DatumParameters(optDatum, datumTransform, None, a, 1 - (b * b) / (a * a))

  def setES(es: Double): DatumParameters =
    DatumParameters(optDatum, datumTransform, None, a, es)

  def setRF(rf: Double): DatumParameters =
    DatumParameters(optDatum, datumTransform, None, a, rf * (2 - rf))

  def setRA: DatumParameters =
    DatumParameters(optDatum, datumTransform, None,
      a * (1 - es * (SIXTH + es * (RA4 + es * RA6))), es)

  def setF(f: Double) =
    DatumParameters(optDatum, datumTransform, None, a, (1 / f) * (2 - (1 / f)))

}
