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

import geotrellis.proj4.util.ProjectionMath._

import collection.immutable.Map

object Proj4Parser {

  def apply(registry: Registry): Proj4Parser = new Proj4Parser(registry)

}

class Proj4Parser(private val registry: Registry) {

  def parse(name: String, args: Array[String]): CoordinateReferenceSystem = {
    val parameters = createParameterMap(args)

    if (!isValid(parameters.keySet))
      throw new IllegalArgumentException(
        "One or more of the arguments provided isn't valid."
      )

    var datumParameters = parseDatum(parameters, new DatumParameters())
    datumParameters = parseEllipsoid(parameters, datumParameters)
    val datum = datumParameters.datum
    val ellipsoid = datumParameters.ellipsoid

    val projection = parseProjection(parameters, ellipsoid)

    CoordinateReferenceSystem(name, Some(args), Some(datum), Some(projection))
  }

  private def parseProjection(
    parameters: Map[String, Option[String]],
    ellipsoid: Ellipsoid): Projection = {
    var projection = parameters.get(Proj4Keyword.proj) match {
      case Some(code) =>
        registry.getProjection(code) getOrElse {
          throw new InvalidValueException("Unknown projection: $code.")
        }
      case None => throw new IllegalStateException("No proj flag specified.")
    }

    projection = projection.set(ellipsoid)

    projection = params.get(Proj4Keyword.alpha) match {
      case Some(s) => projection.setAlphaDegrees(s.toDouble)
      case None => projection
    }

    projection = params.get(Proj4Keyword.lonc) match {
      case Some(s) => projection.setLonCDegrees(s.toDouble)
      case None => projection
    }

    projection = params.get(Proj4Keyword.lat_0) match {
      case Some(s) => projection.setProjectionLatitudeDegrees(parseAngle(s))
      case None => projection
    }

    projection = params.get(Proj4Keyword.lon_0) match {
      case Some(s) => projection.setProjectionLongitudeDegrees(parseAngle(s))
      case None => projection
    }

    projection = params.get(Proj4Keyword.lat_1) match {
      case Some(s) => projection.setProjectionLatitude1Degrees(parseAngle(s))
      case None => projection
    }

    projection = params.get(Proj4Keyword.lat_2) match {
      case Some(s) => projection.setProjectionLatitude2Degrees(parseAngle(s))
      case None => projection
    }

    projection = params.get(Proj4Keyword.lat_ts) match {
      case Some(s) => projection.setTrueScaleLatitudeDegrees(parseAngle(s))
      case None => projection
    }

    projection = params.get(Proj4Keyword.x_0) match {
      case Some(s) => projection.setFalseEasting(s.toDouble)
      case None => projection
    }

    projection = params.get(Proj4Keyword.y_0) match {
      case Some(s) => projection.setFalseNorthing(s.toDouble)
      case None => projection
    }

    projection = params.get(Proj4Keyword.k_0) match {
      case Some(s) => projection.setScaleFactor(s.toDouble)
      case None => params.get(Proj4Keyword.k) match {
        case Some(s) => projection.setScaleFactor(s.toDouble)
        case None => projection
      }
    }

    projection = params.get(Proj4Keyword.units) match {
      case Some(code) => Units.findUnits(code) match {
        case Some(unit) =>
          projection.setFromMetres(1 / unit.value).setUnits(unit)
        case None =>
          throw new InvalidValueException(s"Unknown unit: $code.")
      }
      case None => projection
    }

    projection = params.get(Proj4Keyword.to_meter) match {
      case Some(s) => projection.setFromMetres(1 / s.toDouble)
      case None => projection
    }

    if (params.contains(Proj4Keyword.south)) {
      projection = projection.setSouthernHemisphere(true)

      projection = projection match {
        case p: TransverseMercatorProjection => p.setUTMZone(s.toInt)
        case _ => projection
      }
    }

    projection.initialize
  }

  private def parseDatum(
    parameters: Map[String, Option[String]],
    datumParametersInput: DatumParameters): DatumParameters = {
    var datumParameters = params.get(Proj4Keyword.towgs84) match {
      case Some(toWGS84) =>
        datumParametersInput.setDatumTransform(parseToWGS84(toWGS84))
      case None => datumParametersInput
    }

    params.get(Proj4Keyword.datum) match {
      case Some(code) => registry.getDatum(code) match {
        case Some(datum) => datumParameters.setDatum(datum)
        case None => throw new InvalidValueException(s"Unknown datum: $datum.")
      }
      case None => datumParameters
    }
  }

  private def parseToWGS84(parameterList: String): Array[Double] = {
    var parameters = parameterList.split(",").map(_.toDouble)

    if (parameters.size != 3 && parameters.size != 7)
      throw new InvalidValueException(
        s"Invalid number of values (must be 3 or 7) in toWGS84: $parameterList."
      )

    if (parameters.length > 3) {
      if (parameters(3) == 0 && parameters(4) == 0 &&
        parameters(5) == 0 && parameters(6) == 0) parameters.take(3)
      else parameters.take(3) ++ parameters.drop(3).take(3)
        .map(_ * SECONDS_TO_RAD) :+ parameters(6) / MILLION + 1
    } else parameters
  }

  private def parseEllipsoid(
    parameters: Map[String, Option[String]],
    datumParametersInput: DatumParameters): DatumParameters = {

    var datumParameters = parameters.get(Proj4Keyword.ellps) match {
      case Some(code) => registry.getEllipsoid(code) match {
        case Some(e) => datumParametersInput.setEllipsoid(e)
        case None => throw new InvalidValueException(
          s"Unknown ellipsoid: $code"
        )
      }
      case None => datumParametersInput
    }

    datumParameters = parameters.get(Proj4Keyword.a) match {
      case Some(s) => datumParameters.setA(s.toDouble)
      case None => datumParameters
    }

    datumParameters = parameters.get(Proj4Keyword.es) match {
      case Some(s) => datumParameters.setES(s.toDouble)
      case None => datumParameters
    }

    datumParameters = parameters.get(Proj4Keyword.rf) match {
      case Some(s) => datumParameters.setRF(s.toDouble)
      case None => datumParameters
    }

    datumParameters = parameters.get(Proj4Keyword.f) match {
      case Some(s) => datumParameters.setF(s.toDouble)
      case None => datumParameters
    }

    datumParameters = parameters.get(Proj4Keyword.b) match {
      case Some(s) => datumParameters.setB(s.toDouble)
      case None => datumParameters
    }

    parseEllipsoidRA(parameters, datumParameters)
  }

  private def parseEllipsoidRA(
    parameters: Map[String, Option[String]],
    datumParameters: DatumParameters): DatumParameters =
    if (parameters.contains(Proj4Keyword.R_A)) datumParameters.setR_A
    else datumPrameters

  private def createParameterMap(args: String): Map[String, Option[String]] =
    args.map(x => if (x.startsWith("+")) x.substring(1) else x).map(x => {
      val index = x.indexOf('=')
      if (index != -1) (x.substring(0, index) -> Some(x.substring(index + 1)))
      else (x -> None)
    }).groupBy(_._1).map { case (a, b) => (a, b.head._2) }

  private def parseAngle(s: String): Double = Angle.parse(s)

}
