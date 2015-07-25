package geotrellis.proj4.io.wkt

import geotrellis.proj4.Memoize

import scala.collection.mutable
import org.osgeo.proj4j.NotFoundException

import scala.io.Source

/**
* @author Manuri Perera
*/
object WKT {
  private val file = "proj4/src/main/resources/wkt/epsg.properties"

  private lazy val EPSGCodeToWKTMap = new Memoize[String,String](retrieveWKTStringFromFile,mutable.Map.empty[String,String])

  private lazy val WKTToEPSGCodeMap = new Memoize[String,String](retrieveEPSGCodeFromFile,mutable.Map.empty[String,String])

  /**
   * Returns the WKT representation given an EPSG code in the format EPSG:[number]
   * @param code
   * @return
   */
  def fromEPSGCode(code :String): String ={
    EPSGCodeToWKTMap(code)
  }

  /**
   * Returns the numeric code of a WKT string given the authority
   * @param wktString
   * @return
   */
  def getEPSGCode(wktString:String)={
    WKTToEPSGCodeMap(wktString)
  }

  private def retrieveWKTStringFromFile(code:String):String= {
    Source.fromFile(file).getLines().find(_.split("=")(0) == code) match {
      case Some(string) =>
        val array = string.split("=")
        array(1)
      case None =>
        throw new NotFoundException(s"Unable to find WKT representation of EPSG:$code")
    }
  }

  private def retrieveEPSGCodeFromFile(wktString:String):String = {
    Source.fromFile(file).getLines().find(_.split("=")(1) == wktString) match {
      case Some(string) =>
        val array = string.split("=")
        s"EPSG:${array(0)}"
      case None =>
        throw new NotFoundException(s"Unable to find the EPSG code of $wktString")
    }
  }
}
