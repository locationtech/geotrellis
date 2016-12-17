package geotrellis.proj4.io.wkt

import scala.io.Source
import geotrellis.proj4.Memoize
import org.osgeo.proj4j.NotFoundException
import geotrellis.proj4.io.wkt.Node
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.combinator._

object WKTParser extends RegexParsers {
  // private lazy val wkttoepsgcodemap = new Memoize[String, String]
  private val wktResourcePath = "/geotrellis/proj4/wkt/epsg.properties"
  private var currentSet: scala.collection.mutable.Set[Any] = scala.collection.mutable.Set.empty

  private def withWktFile[T](f: Iterator[String] => T) = {
    val stream = getClass.getResourceAsStream(wktResourcePath)
    try {
            f(Source.fromInputStream(stream).getLines())
    } finally {
            stream.close()
    }
  }

  override def skipWhitespace: Boolean = false
  def symbol: Parser[String] = """[A-Za-z0-9_]+""".r

  def string: Parser[String] = "\"" ~> """[^\"]+""".r <~ "\"" //"""[A-Za-z()/+ 0-9\-&'\,\.\*\?\_]+""".r

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

  def toWgs: Parser[ToWgs84] = """TOWGS84[""" ~ (double <~ ", ".?).* ~ """]""" map {
    case _ ~ x ~ _ => new ToWgs84(x)
  }

  def datum: Parser[Datum] = """DATUM[""" ~ string ~ comma ~ spheroid ~ comma.? ~ toWgs.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ spheroid ~_ ~ toWgs ~ _ ~ authority ~ _ => new Datum(name, spheroid, toWgs, authority)
  }

  def primeM: Parser[PrimeM] = """PRIMEM[""" ~ string ~ comma ~ double ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ longitude ~ _ ~ authority ~ _ => new PrimeM(name, longitude, authority)
  }

  def direction: Parser[String] = "NORTH" | "SOUTH" | "EAST" | "WEST" | "UP" | "DOWN" | "NORTH_EAST" | "GEOCENTRIC_X" | "GEOCENTRIC_Y" | "GEOCENTRIC_Z" | "NORTH_WEST"| "\"North along 30 deg East\"" | "\"North along 120 deg East\"" | "North along 165 deg East\"" | "\"North along 75 deg East\"" | "\"North along 45 deg East\"" | "\"North along 135 deg East\"" | "\"North along 45 deg West\"" | "\"North along 105 deg East\"" | "\"North along 15 deg East\"" |"\"North along 15 deg West\"" | "\"North along 75 deg East\"" | "\"North along 45 deg East\"" | "\"North along 15 deg East\"" | "\"North along 75 deg West\"" | "\"North along 15 deg West\""| "\"North along 160 deg East\""| "\"North along 105 deg West\"" | "\"North along 45 deg West\"" | "\"North along 135 deg West\"" |"\"North along 70 deg East\"" | "\"South along 180 deg\"" | "\"North along 90 deg East\"" | "\"North along 0 deg\"" | "\"North along 90 deg East\"" | "\"North along 0 deg\"" | "\"South along 90 deg West\"" | "\"North along 135 deg West\"" | "\"North along 135 deg East\"" | "\"South along 10 deg West\"" | "\"South along 80 deg East\"" | "\"South along 90 deg East\"" | "\"South along 108 deg East\"" | "\"South along 75 deg West\"" | "\"South along 165 deg West\"" | "\"South along 162 deg West\"" | "\"South along 180 def\"" | "\"South along 60 deg West\"" | "\"South along 30 deg East\"" | "\"South along 57 deg East\"" | "\"South along 147 deg East\"" | "OTHER" ^^ {_.toString}

  def axis: Parser[Axis] = """AXIS[""" ~ string ~ comma ~ (string | symbol) ~ """]""" map {
    case _ ~ name ~ _ ~ direction ~ _ => new Axis(name, direction)
  }

  def twinAxis: Parser[TwinAxis] = axis ~ ", " ~ axis map {
    case x ~ _ ~ y => new TwinAxis(x, y)
  }

  def unit: Parser[Unit] = """UNIT[""" ~ string ~ comma ~ double ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ conversion ~ _ ~ authority ~ _ => new Unit(name, conversion, authority)
  }

  def vertDatum: Parser[VertDatum] = """VERT_DATUM[""" ~ string ~ comma ~ int ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datumType ~ _ ~ authority ~ _ => new VertDatum(name, datumType, authority)
  }

  def geogcs: Parser[Geogcs] = """GEOGCS[""" ~ string ~ comma ~ datum ~ comma ~ primeM ~ comma ~ unit ~ comma.? ~ getAxisList.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datum ~ _ ~ primeM ~_ ~ unit ~ _~ axes ~ _ ~ auth ~ _ => new Geogcs(name, datum, primeM, unit, axes, auth)
  }

  def projection: Parser[Projection] = """PROJECTION[""" ~ string ~ comma.? ~ authority.? ~ """]""" map {
    case _  ~ name ~ _ ~ authority ~ _ => new Projection(name, authority)
  }

  def parameterList: Parser[List[Parameter]] = ((parameter ~ comma.?)*) ^^ (_.map(_._1))

  def projcs: Parser[Projcs] = """PROJCS[""" ~ string ~ comma ~ geogcs ~ comma ~ projection ~ comma.? ~ parameterList.? ~ comma.? ~ unit ~ comma.? ~ twinAxis.? ~ comma.? ~ authority.? ~ """]""" map {
    case _  ~ name ~  _ ~ geogcs ~ _ ~ projection ~ _ ~ params ~ _  ~ unit ~ _ ~ twin ~ _ ~ authority ~ _ => new Projcs(name, geogcs, projection, params, unit, twin, authority)
  }

  def vertcs: Parser[VertCS] = """VERT_CS[""" ~ string ~ comma ~ vertDatum ~ comma ~ unit ~ comma.? ~ axis.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ vertDatum ~ _ ~ unit ~ _ ~ axis ~ _ ~ authority ~ _ => new VertCS(name, vertDatum, unit, axis, authority)
  }

  def localDatum: Parser[LocalDatum] = """LOCAL_DATUM[""" ~ string ~ comma ~ int ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datumType ~ _ ~ authority ~ _ => new LocalDatum(name, datumType, authority)
  }

  def getAxisList: Parser[List[Axis]] = ((axis ~ comma.?)+)^^ (_.map(_._1))

  def localCS: Parser[LocalCS] = """LOCAL_CS[""" ~ string ~ comma ~ localDatum ~ comma ~ unit ~ comma ~ getAxisList ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ localDatum ~_ ~ unit ~ _ ~ axisList ~ _ ~ authority ~ _ => new LocalCS(name, localDatum, unit, axisList, authority)
  }

  def geoccs: Parser[Geoccs] = """GEOCCS[""" ~ string ~ comma ~ datum ~ comma ~ primeM ~ comma ~ unit ~ comma.? ~ getAxisList.? ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ datum ~ _ ~ primeM ~ _ ~ unit ~ _ ~ getAxisList ~ _ ~ auth ~ _ => new Geoccs(name, datum, primeM, unit, getAxisList, auth)
  }

  def coordinateSys: Parser[Any] = geogcs | geoccs | projcs | vertcs | compdcs | localCS

  def compdcs: Parser[CompDCS] = """COMPD_CS[""" ~ string ~ comma ~ coordinateSys ~ comma ~ coordinateSys ~ comma.? ~ authority.? ~ """]""" map {
    case _ ~ name ~ _ ~ head ~ _ ~ tail ~ _ ~ auth ~ _ => new CompDCS(name, head, tail, auth)
  }

  def parseWKT: Parser[Tree] = localCS | projcs | geogcs | geoccs | compdcs | vertcs

  def createTree() = {
    //read input from epsg.properties file
    var count = 0;
    withWktFile { lines =>
      for (line <- lines) {
        //split the line for parsing the wkt
        val array = line.split("=")
        val code = array(0)
        val wkt = array(1)
        //parse the wkt string
        parseAll(parseWKT, wkt) match {
          case Success(wktObject, _) => {
            currentSet += wktObject
          }
          case Failure(msg, tail) => {
            val sb = new StringBuilder()
            sb.append(tail)
            println("FAILURE: " + msg + "\n" + sb.toString() + "\n\n" + wkt + "\n")
            count += 1

          }
          case Error(msg, _) => println("ERROR: " + msg)
        }
      }
    }
    println("COUNT " + count)
  }

  def containsObject(input: Any): Boolean = {
    currentSet contains input
  }
}