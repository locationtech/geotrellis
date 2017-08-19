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

import scala.io.Source

object WKT {
  private val wktResourcePath = "/geotrellis/proj4/wkt/epsg.properties"
  lazy val parsed: Map[Int, WktCS] = records.mapValues(WKTParser.apply)
  lazy val projections: Set[WktCS] = parsed.values.toSet
  lazy val records: Map[Int, String] = parseWktEpsgResource

  def parseWktEpsgResource(): Map[Int, String] = {
    //read input from epsg.properties file
    WKT.withWktFile { lines =>
      val iter =
        for (line <- lines) yield {
          //split the line for parsing the wkt, aka remove code infront
          val firstEquals = line.indexOf("=")
          val code = line.substring(0, firstEquals).toInt
          val wktString = line.substring(firstEquals + 1)
          code -> wktString
        }
      iter.toMap
    }
  }

  def contains(input: WktCS): Boolean = {
    projections contains input
  }

  def getEpsgCodeOption(input: String): Option[Int] = {
    val wktParsed = WKTParser(input)
    parsed.find{
      case (epsgCode, wkt) => wkt == wktParsed
    }.map(_._1)
  }

  def getEpsgStringOption(input: Int): Option[String] = {
    records.get(input).map(_.toString)
  }


  /**
   * Returns the WKT representation given an EPSG code in the format EPSG:[number]
   * @param code
   * @return
   */
  def fromEpsgCode(code: Int): String = getEpsgStringOption(code).get

  /**
   * Returns the numeric code of a WKT string given the authority
   * @param wktString
   * @return
   */
  def getEpsgCode(wktString: String): String = s"EPSG:${getEpsgCodeOption(wktString).get}"

  def withWktFile[T](f: Iterator[String] => T) = {
    val stream = getClass.getResourceAsStream(wktResourcePath)
    try {
      f(Source.fromInputStream(stream).getLines())
    } finally {
      stream.close()
    }
  }

}
