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

package geotrellis.proj4.io.wkt

sealed trait WktCS

case class Authority(name: String, code: String)

case class ToWgs84(values:List[Double])

case class Parameter(name: String, value: Number)

case class Axis(name: String, direction: String)

case class TwinAxis(first: Axis, second: Axis)

case class Spheroid(name: String, semiMajorAxis: AnyVal, flatteningInverse: Double, authority: Option[Authority])

case class Datum(name: String, spheroid: Spheroid, towsg: Option[ToWgs84], authority: Option[Authority])

case class UnitField(name: String, conversionFact: Number, authority: Option[Authority])

case class PrimeM(name: String, longitude: Double, authority: Option[Authority])

case class VertDatum(name: String, datumType: Number, authority: Option[Authority])

case class LocalDatum(name: String, datumType: Number, authority: Option[Authority])

case class Projection(name: String, authority: Option[Authority])

case class GeogCS(name: String, datum: Datum, primeM: PrimeM, angularUnit: UnitField, axes: Option[List[Axis]], authority: Option[Authority]) extends WktCS

case class ProjCS(name: String, geogcs: GeogCS, projection: Projection, params: Option[List[Parameter]], linearUnit: UnitField, twinAxis: Option[TwinAxis], authority: Option[Authority]) extends WktCS

case class VertCS(name: String, vertDatum: VertDatum, unit: UnitField, axis: Option[Axis], authority: Option[Authority]) extends WktCS

case class LocalCS(name: String, localDatum: LocalDatum, unit: UnitField, axisList: List[Axis], authority: Option[Authority]) extends WktCS

case class GeocCS(name: String, datum: Datum, primeM: PrimeM, unit: UnitField, axis: Option[List[Axis]], authority: Option[Authority]) extends WktCS

case class CompDCS(name: String, head: Any, tail: Any, authority: Option[Authority]) extends WktCS