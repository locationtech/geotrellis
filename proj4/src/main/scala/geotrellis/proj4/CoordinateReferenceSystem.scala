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

import geotrellis.proj4.datum._

import org.osgeo.proj4j.proj._

object CoordinateReferenceSystem {
  def apply(datum: Datum, projection: Projection): CoordinateReferenceSystem =
    new CoordinateReferenceSystem(datum, projection)

  def apply(name: String, datum: Datum, projection: Projection): CoordinateReferenceSystem =
    new CoordinateReferenceSystem(datum, projection, Some(name))

  def apply(name: String, datum: Datum, projection: Projection, inputParameters: Array[String]): CoordinateReferenceSystem =
    new CoordinateReferenceSystem(datum, projection, Some(name), Some(inputParameters))
}

class CoordinateReferenceSystem(
  val datum: Datum,
  val projection: Projection,
  inputName: Option[String] = None,
  inputParameters: Option[Array[String]] = None
) {

  val name = 
    inputName match {
      case Some(n) => n
      case _ => s"${projection.getName}-CS"
    }

  val parameters: Array[String] =
    inputParameters match {
      case Some(ip) => ip
      case _ =>
        /** TODO: Should be able to create parameter string from CRS info */
        Array[String]()
    }

  val parameterString = 
    parameters.mkString(" ").trim

  def createGeographic(): CoordinateReferenceSystem = {
    val geoProjection = new LongLatProjection
    val crsName = "GEO-" + datum.code

    CoordinateReferenceSystem(crsName, datum, geoProjection)
  }

  override def toString = name
}
