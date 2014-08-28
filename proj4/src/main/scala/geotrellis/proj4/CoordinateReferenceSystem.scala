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

package geotrellis.proj4

object CoordinateReferenceSystem {

  val CS_GEO = new CoordinateReferenceSystem("CS_GEO")

}

case class CoordinateReferenceSystem(
  inputName: String = "null-proj",
  parameters: Option[Array[String]] = None,
  datum: Option[Datum] = None,
  projection: Option[Projection] = None
) {

  val name = projection match {
    case Some(proj) if (inputName == "null-proj") => s"${proj.getName}-CS"
    case _ => inputName
  }

  val parameterString = params match {
    case Some(params) => params.foldLeft("")((a, b) => s"$a $b").trim
    case None => ""
  }

  def createGeographic: CoordinateReferenceSystem = {
    val ellipsoid = projection match {
      case Some(proj) => Some(proj.ellipsoid)
      case None => None
    }

    val geoProjection = new LongLatProjection(ellipsoid, Units.DEGREES)
    val crsName = = datum match {
      case Some(d) => "GEO-" + d.getCode
      case None => "null-proj"
    }

    new CoordinateReferenceSystem(crsName, None, datum, geoProjection)
  }

  override def toString = name

}
