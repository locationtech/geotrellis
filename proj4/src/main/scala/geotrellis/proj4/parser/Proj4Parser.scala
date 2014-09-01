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

import geotrellis.proj4._
import geotrellis.proj4.units._
import geotrellis.proj4.datum._
import geotrellis.proj4.util.ProjectionMath._

import org.osgeo.proj4j.proj._

import collection.immutable.Map

object Proj4Parser {
  private def createParameterMap(args: Seq[String]): Map[String, Option[String]] =
    args
      .map { arg => 
        val x =
          if (arg.startsWith("+")) arg.substring(1)
          else arg

        val index = x.indexOf('=')

        if (index != -1) (x.substring(0, index) -> Some(x.substring(index + 1)))
        else (x -> None)
       }
      .toMap

  def parse(name: String, args: Array[String]): CoordinateReferenceSystem = {
    val params = createParameterMap(args)

    Proj4Keyword.invalidKeys(params.keySet) match {
      case Some(keys) =>
        throw new IllegalArgumentException(
          s"""Invalid proj4 argument(s): ${keys.mkString(", ")}."""
        )
      case None =>
    }

    val datum @ Datum(_, ellipsoid, _, _) =
      params.get(Proj4Keyword.datum) match {
        case Some(Some(code)) =>
          Registry.getDatum(code) match {
            case Some(datum) =>
              datum
            case None =>
              throw new InvalidValueException(s"Unknown datum: $code.")
          }
        case _ =>
          val ellipsoid = parseEllipsoid(params)

          if(ellipsoid == Ellipsoid.NULL || ellipsoid == Ellipsoid.WGS84) 
            Datum.WGS84
          else {
            val transform =
              params.get(Proj4Keyword.towgs84) match {
                case Some(Some(toWGS84)) =>
                  parseToWGS84(toWGS84)
                case _ =>
                  Datum.Constants.DEFAULT_TRANSFORM
              }

            Datum("User", ellipsoid, "User-defined", transform)
          }
      }

    val projection = parseProjection(params, ellipsoid)

    CoordinateReferenceSystem(name, datum, projection, args)
  }

  private def parseProjection(params: ProjParams, ellipsoid: Ellipsoid): Projection = {
    params.get(Proj4Keyword.proj) match {
      case Some(codeOpt) =>
        codeOpt match {
          case Some(code) =>
            val builder = 
              Registry.getProjectionBuilder(code) getOrElse {
                throw new InvalidValueException("Unknown projection: $code.")
              }
            builder.setProj4Params(params).build
          case None =>
            throw new IllegalStateException("No proj code specified.")
        }
      case None => throw new IllegalStateException("No proj flag specified.")
    }

    ???
  }

// {
//     var projection = 
//       parameters.get(Proj4Keyword.proj) match {
//         case Some(codeOpt) =>
//           codeOpt match {
//             case Some(code) =>
//               Registry.getProjection(code) getOrElse {
//                 throw new InvalidValueException("Unknown projection: $code.")
//               }
//             case None =>
//               throw new IllegalStateException("No proj code specified.")
//           }
//       case None => throw new IllegalStateException("No proj flag specified.")
//     }

//     projection = projection.setEllipsoid(ellipsoid)

//     projection = parameters.get(Proj4Keyword.alpha) match {
//       case Some(s) => projection.setAlphaDegrees(s.toDouble)
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lonc) match {
//       case Some(s) => projection.setLonCDegrees(s.toDouble)
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lat_0) match {
//       case Some(s) => projection.setProjectionLatitudeDegrees(Angle.parse(s))
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lon_0) match {
//       case Some(s) => projection.setProjectionLongitudeDegrees(Angle.parse(s))
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lat_1) match {
//       case Some(s) => projection.setProjectionLatitude1Degrees(Angle.parse(s))
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lat_2) match {
//       case Some(s) => projection.setProjectionLatitude2Degrees(Angle.parse(s))
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.lat_ts) match {
//       case Some(s) => projection.setTrueScaleLatitudeDegrees(Angle.parse(s))
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.x_0) match {
//       case Some(s) => projection.setFalseEasting(s.toDouble)
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.y_0) match {
//       case Some(s) => projection.setFalseNorthing(s.toDouble)
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.k_0) match {
//       case Some(s) => projection.setScaleFactor(s.toDouble)
//       case None => parameters.get(Proj4Keyword.k) match {
//         case Some(s) => projection.setScaleFactor(s.toDouble)
//         case None => projection
//       }
//     }

//     projection = parameters.get(Proj4Keyword.units) match {
//       case Some(code) => Units.findUnits(code) match {
//         case Some(unit) =>
//           projection.setFromMetres(1 / unit.value).setUnits(unit)
//         case None =>
//           throw new InvalidValueException(s"Unknown unit: $code.")
//       }
//       case None => projection
//     }

//     projection = parameters.get(Proj4Keyword.to_meter) match {
//       case Some(s) => projection.setFromMetres(1 / s.toDouble)
//       case None => projection
//     }

//     if (parameters.contains(Proj4Keyword.south)) {
//       projection = projection.setSouthernHemisphere(true)

//       projection = projection match {
//         case p: TransverseMercatorProjection => p.setUTMZone(s.toInt)
//         case _ => projection
//       }
//     }

//     projection.initialize
//   }

  implicit class EllipsoidBuilderProj4Wrapper(eb: EllipsoidBuilder) {
    def setDouble(params: ProjParams, key: String)(set: (EllipsoidBuilder, Double) => EllipsoidBuilder): EllipsoidBuilder =
      params.get(key) match {
        case Some(Some(s)) => set(eb, s.toDouble)
        case _ => eb
      }

    def setExists(params: ProjParams, key: String)(set: EllipsoidBuilder => EllipsoidBuilder): EllipsoidBuilder =
      if(params.contains(key)) set(eb)
      else eb
  }

  private def parseEllipsoid(params: ProjParams): Ellipsoid = {
    params.get(Proj4Keyword.ellps) match {
      case Some(Some(code)) =>
        Registry.getEllipsoid(code) match {
          case Some(e) => e
          case None =>
            throw new InvalidValueException(s"Unknown ellipsoid: $code")
        }
      case _ =>
        new EllipsoidBuilder()
          .setDouble(params, Proj4Keyword.a)(_.setA(_))
          .setDouble(params, Proj4Keyword.es)(_.setES(_))
          .setDouble(params, Proj4Keyword.rf)(_.setRF(_))
          .setDouble(params, Proj4Keyword.f)(_.setF(_))
          .setDouble(params, Proj4Keyword.b)(_.setB(_))
          .setExists(params, Proj4Keyword.R_A)(_.setRA())
          .build
    }
  }

  private def parseToWGS84(parameterList: String): Vector[Double] = {
    var parameters = 
      parameterList.split(",").map(_.toDouble).toVector

    if (parameters.size != 3 && parameters.size != 7)
      throw new InvalidValueException(
        s"Invalid number of values (must be 3 or 7) in toWGS84: $parameterList."
      )

    if (parameters.length > 3) {
      if (parameters(3) == 0.0 && parameters(4) == 0.0 &&
        parameters(5) == 0.0 && parameters(6) == 0.0) {
        parameters.take(3)
      } else {
        parameters.take(3) ++ parameters.drop(3).take(3)
        .map(_ * SECONDS_TO_RAD) :+ parameters(6) / MILLION + 1
      }
    } else {
      parameters
    }
  }
}
