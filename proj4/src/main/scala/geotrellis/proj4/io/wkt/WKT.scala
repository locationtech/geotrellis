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
