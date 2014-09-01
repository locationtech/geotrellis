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

import monocle.syntax._

import collection.immutable.Map

import geotrellis.proj4.util.ProjectionMath._
import geotrellis.proj4.proj.Projection
import geotrellis.proj4.proj.ProjectionBuilderLenses._
import geotrellis.proj4.proj.ProjectionBuilders._
import geotrellis.proj4.units._
import geotrellis.proj4.datum._
import geotrellis.proj4._

object Proj4Parser {

  def apply(registry: Registry): Proj4Parser = new Proj4Parser(registry)

}

class Proj4Parser(private val registry: Registry) {

  def parse(name: String, args: Array[String]): CoordinateReferenceSystem = {
    val parameters = createParameterMap(args)

    if (!Proj4Keyword.isValid(parameters.keySet))
      throw new IllegalArgumentException(
        "One or more of the arguments provided isn't valid."
      )

    var datumParameters = parseDatum(parameters, new DatumParameters())
    datumParameters = parseEllipsoid(parameters, datumParameters)
    val datum = datumParameters.datum
    val ellipsoid = datumParameters.ellipsoid

    val projection = parseProjection(parameters, ellipsoid)

    CoordinateReferenceSystem(name, Some(args), datum, projection)
  }

  private def parseProjection(
    parameters: Map[String, String],
    ellipsoid: Ellipsoid): Projection = {
    var projectionBuilder = parameters.get(Proj4Keyword.proj) match {
      case Some(code) =>
        registry.getProjectionBuilder(code) getOrElse {
          throw new InvalidValueException("Unknown projection: $code.")
        }
      case None => throw new IllegalStateException("No proj flag specified.")
    }

    projectionBuilder = projectionBuilder |-> ellipsoidLens set ellipsoid

    projectionBuilder = parameters.get(Proj4Keyword.alpha) match {
      case Some(s) => projectionBuilder.setAlphaDegrees(s.toDouble)
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lonc) match {
      case Some(s) => projectionBuilder.setLonCDegrees(s.toDouble)
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lat_0) match {
      case Some(s) => projectionBuilder.setProjectionLatitudeDegrees(parseAngle(s))
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lon_0) match {
      case Some(s) => projectionBuilder.setProjectionLongitudeDegrees(parseAngle(s))
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lat_1) match {
      case Some(s) => projectionBuilder.setProjectionLatitude1Degrees(parseAngle(s))
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lat_2) match {
      case Some(s) => projectionBuilder.setProjectionLatitude2Degrees(parseAngle(s))
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.lat_ts) match {
      case Some(s) => projectionBuilder.setTrueScaleLatitudeDegrees(parseAngle(s))
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.x_0) match {
      case Some(s) => projectionBuilder |-> falseEastingLens set s.toDouble
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.y_0) match {
      case Some(s) => projectionBuilder |-> falseNorthingLens set s.toDouble
      case None => projectionBuilder
    }

    val scaleFactor = parameters.get(Proj4Keyword.k_0) match {
      case Some(s) => s.toDouble
      case None => parameters.get(Proj4Keyword.k) match {
        case Some(s) => s.toDouble
        case None => projectionBuilder.scaleFactor
      }
    }

    projectionBuilder = projectionBuilder |-> scaleFactorLens set scaleFactor

    projectionBuilder = parameters.get(Proj4Keyword.units) match {
      case Some(code) => {
        val unit = Units.findUnits(code)
          projectionBuilder = projectionBuilder |-> fromMetresLens set 1 / unit.value
          projectionBuilder = projectionBuilder |-> unitOptionLens set Some(unit)

          projectionBuilder
        }
      case None => projectionBuilder
    }

    projectionBuilder = parameters.get(Proj4Keyword.to_meter) match {
      case Some(s) => projectionBuilder |-> fromMetresLens set 1 / s.toDouble
      case None => projectionBuilder
    }

    if (parameters.contains(Proj4Keyword.south)) {
      projectionBuilder = projectionBuilder |-> isSouthLens set true

      projectionBuilder = projectionBuilder.name match {
        case tmercProjectionName | utmProjectionName =>
          parameters.get(Proj4Keyword.zone) match {
            case Some(s) => projectionBuilder.setUTMZone(s.toInt)
            case None => projectionBuilder
          }
        case _ => projectionBuilder
      }
    }

    projectionBuilder.build
  }

  private def parseDatum(
    parameters: Map[String, String],
    datumParametersInput: DatumParameters): DatumParameters = {
    var datumParameters = parameters.get(Proj4Keyword.towgs84) match {
      case Some(toWGS84) =>
        datumParametersInput.setDatumTransform(parseToWGS84(toWGS84))
      case None => datumParametersInput
    }

    parameters.get(Proj4Keyword.datum) match {
      case Some(code) => registry.getDatum(code) match {
        case Some(datum) => datumParameters.setDatum(datum)
        case None => throw new InvalidValueException(s"Unknown datum: $code.")
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
    parameters: Map[String, String],
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
    parameters: Map[String, String],
    datumParameters: DatumParameters): DatumParameters =
    if (parameters.contains(Proj4Keyword.R_A)) datumParameters.setRA
    else datumParameters

  private def createParameterMap(args: Array[String]): Map[String, String] =
    args.map(x => if (x.startsWith("+")) x.substring(1) else x).map(x => {
      val index = x.indexOf('=')
      if (index != -1) (x.substring(0, index) -> Some(x.substring(index + 1)))
      else (x -> None)
    }).groupBy(_._1).map { case (a, b) => (a, b.head._2) }
      .filter { case (a, b) => b.isEmpty }.map { case (a, b) => (a -> b.get) }

  private def parseAngle(s: String): Double = Angle.parse(s)

}
