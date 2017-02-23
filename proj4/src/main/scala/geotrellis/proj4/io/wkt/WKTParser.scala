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

import scala.util.parsing.combinator.RegexParsers

object WKTParser extends RegexParsers {

  override def skipWhitespace: Boolean = false

  def symbol: Parser[String] = """[A-Za-z0-9_]+""".r

  def string: Parser[String] = "\"" ~> """[A-Za-z=()/+ 0-9-&',.*?\_]+""".r <~ "\""

  def double: Parser[Double] =("""[-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?""".r | """[-+]?[0-9]*\.?[0-9]+""".r) map (_.toDouble)

  def int: Parser[Int] = """[-+]?[0-9]+""".r map (_.toInt)

  def value: Parser[Any] = string | double | int

  def comma: Parser[Any] = ("," ~ " ".?) map {
    case x ~ _ => None
  }

  def parameter: Parser[Parameter] = """PARAMETER[""" ~ string ~ comma ~ double ~ """]""" map {
    case _ ~ name ~ _ ~ value ~ _=> new Parameter(name, value)
  }

  def authority: Parser[Authority] = """AUTHORITY[""" ~ string ~ comma ~ string ~ """]""" map {
    case _ ~ x ~ _~ y ~ _ => new Authority(x, y)
  }

  def spheroid: Parser[Spheroid] = """SPHEROID[""" ~ string ~ comma ~ (double | int) ~ comma ~ double ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ axis ~ _ ~ flattening ~ _ ~ authority ~ _ => new Spheroid(name, axis, flattening, authority)
  }

  def toWgs: Parser[ToWgs84] = """TOWGS84[""" ~ (double <~ comma.?).* ~ """]""" map {
    case _ ~ x ~ _ => new ToWgs84(x)
  }

  def datum: Parser[Datum] = """DATUM[""" ~ string ~ comma ~ spheroid ~ comma.? ~ toWgs.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ spheroid ~_ ~ toWgs ~ _ ~ authority ~ _ => new Datum(name, spheroid, toWgs, authority)
  }

  def primeM: Parser[PrimeM] = """PRIMEM[""" ~ string ~ comma ~ double ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ longitude ~ _ ~ authority ~ _ => new PrimeM(name, longitude, authority)
  }

  def axis: Parser[Axis] = """AXIS[""" ~ string ~ comma ~ (string | symbol) ~ """]""" map {
    case _ ~ name ~ _ ~ direction ~ _ => new Axis(name, direction)
  }

  def twinAxis: Parser[TwinAxis] = axis ~ comma ~ axis map {
    case x ~ _ ~ y => new TwinAxis(x, y)
  }

  def unitField: Parser[UnitField] = """UNIT[""" ~ string ~ comma ~ double ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ conversion ~ _ ~ authority ~ _ => new UnitField(name, conversion, authority)
  }

  def vertDatum: Parser[VertDatum] = """VERT_DATUM[""" ~ string ~ comma ~ int ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datumType ~ _ ~ authority ~ _ => new VertDatum(name, datumType, authority)
  }

  def geogcs: Parser[GeogCS] = """GEOGCS[""" ~ string ~ comma ~ datum ~ comma ~ primeM ~ comma ~ unitField ~ comma.? ~ getAxisList.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datum ~ _ ~ primeM ~_ ~ unitField ~ _~ axes ~ _ ~ auth ~ _ => new GeogCS(name, datum, primeM, unitField, axes, auth)
  }

  def projection: Parser[Projection] = """PROJECTION[""" ~ string ~ comma.? ~ authority.? ~ """]""" map {
    case _  ~ name ~ _ ~ authority ~ _ => new Projection(name, authority)
  }

  def parameterList: Parser[List[Parameter]] = ((parameter ~ comma.?)*) ^^ (_.map(_._1))

  def projcs: Parser[ProjCS] = """PROJCS[""" ~ string ~ comma ~ geogcs ~ comma ~ projection ~ comma.? ~ parameterList.? ~ comma.? ~ unitField ~ comma.? ~ twinAxis.? ~ comma.? ~ authority.? ~ """]""" map {
    case _  ~ name ~  _ ~ geogcs ~ _ ~ projection ~ _ ~ params ~ _  ~ unitField ~ _ ~ twin ~ _ ~ authority ~ _ => new ProjCS(name, geogcs, projection, params, unitField, twin, authority)
  }

  def vertcs: Parser[VertCS] = """VERT_CS[""" ~ string ~ comma ~ vertDatum ~ comma ~ unitField ~ comma.? ~ axis.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ vertDatum ~ _ ~ unitField ~ _ ~ axis ~ _ ~ authority ~ _ => new VertCS(name, vertDatum, unitField, axis, authority)
  }

  def localDatum: Parser[LocalDatum] = """LOCAL_DATUM[""" ~ string ~ comma ~ int ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datumType ~ _ ~ authority ~ _ => new LocalDatum(name, datumType, authority)
  }

  def getAxisList: Parser[List[Axis]] = ((axis ~ comma.?)+)^^ (_.map(_._1))

  def localcs: Parser[LocalCS] = """LOCAL_CS[""" ~ string ~ comma ~ localDatum ~ comma ~ unitField ~ comma ~ getAxisList ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ localDatum ~_ ~ unitField ~ _ ~ axisList ~ _ ~ authority ~ _ => new LocalCS(name, localDatum, unitField, axisList, authority)
  }

  def geoccs: Parser[GeocCS] = """GEOCCS[""" ~ string ~ comma ~ datum ~ comma ~ primeM ~ comma ~ unitField ~ comma.? ~ getAxisList.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datum ~ _ ~ primeM ~ _ ~ unitField ~ _ ~ getAxisList ~ _ ~ auth ~ _ => new GeocCS(name, datum, primeM, unitField, getAxisList, auth)
  }

  def coordinateSys: Parser[Any] = geogcs | geoccs | projcs | vertcs | compdcs | localcs

  def compdcs: Parser[CompDCS] = """COMPD_CS[""" ~ string ~ comma ~ coordinateSys ~ comma ~ coordinateSys ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ head ~ _ ~ tail ~ _ ~ auth ~ _ => new CompDCS(name, head, tail, auth)
  }

  def wktCS: Parser[WktCS] = localcs | projcs | geogcs | geoccs | compdcs | vertcs

  def apply(wktString: String) : WktCS = {
    parseAll(wktCS, wktString) match {
      case Success(wktObject, _) =>
        wktObject
      case Failure(msg, tail) =>
        throw new IllegalArgumentException(msg)
      case Error(msg, _) => {
        throw new IllegalArgumentException(msg)
      }
    }
  }

}