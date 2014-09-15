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

import geotrellis.proj4.datum.Datum
import geotrellis.proj4.proj.Projection

case class CoordinateReferenceSystem(
  inputName: String,
  parameters: Option[Array[String]],
  datum: Datum,
  projection: Projection
) {

  val name = s"${projection.name}-CS"

  val parametersString: Option[String] = parameters match {
    case Some(p) => Some(p.foldLeft("")((a, b) => s"$a $b").trim)
    case None => None
  }

  def createGeographic: CoordinateReferenceSystem = {
    val ellipsoid = projection.ellipsoid

    val geoProjection = new LongLatProjection(ellipsoid, Units.DEGREES)

    val crsName = "GEO-${datum.getCode}"

    new CoordinateReferenceSystem(crsName, None, datum, geoProjection)
  }

  override def toString = name

}
