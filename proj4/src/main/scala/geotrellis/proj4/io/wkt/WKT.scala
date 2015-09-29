package geotrellis.proj4.io.wkt

import geotrellis.proj4.Memoize
import org.osgeo.proj4j.NotFoundException

import scala.io.Source

/**
 * @author Manuri Perera
 */
object WKT {
  private lazy val epsgcodetowktmap = new Memoize[String, String](retrieveWKTStringFromFile)
  private lazy val wkttoepsgcodemap = new Memoize[String, String](retrieveEPSGCodeFromFile)
  private val wktResourcePath = "/geotrellis/proj4/wkt/epsg.properties"

  /**
   * Returns the WKT representation given an EPSG code in the format EPSG:[number]
   * @param code
   * @return
   */
  def fromEPSGCode(code: Int): String = epsgcodetowktmap(code.toString)

  /**
   * Returns the numeric code of a WKT string given the authority
   * @param wktString
   * @return
   */
  def getEPSGCode(wktString: String) = wkttoepsgcodemap(wktString)

  private def withWktFile[T](f: Iterator[String] => T) = {
    val stream = getClass.getResourceAsStream(wktResourcePath)
    try {
      f(Source.fromInputStream(stream).getLines())
    } finally {
      stream.close()
    }
  }

  /**
   * Returns the WKT string for the passed EPSG code
   * @param code
   * @return
   */
  private def retrieveWKTStringFromFile(code: String): String =
    withWktFile { lines =>
      lines.find(_.split("=")(0) == code) match {
        case Some(string) =>
          val array = string.split("=")
          array(1)
        case None =>
          throw new NotFoundException(s"Unable to find WKT representation of EPSG:$code")
      }
    }

  /**
   * Returns the EPSG code for the passed WKT string
   * @param wktString
   * @return
   */
  private def retrieveEPSGCodeFromFile(wktString: String): String =
    withWktFile { lines =>
      lines.find(_.split("=")(1) == wktString) match {
        case Some(string) =>
          val array = string.split("=")
          s"EPSG:${array(0)}"
        case None =>
          throw new NotFoundException(s"Unable to find the EPSG code of $wktString")
      }
    }
}
