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

import geotrellis.proj4.CRS

import scala.io.Source

object WKT {
  private val wktResourcePath = "/proj4/wkt/epsg.properties"
  lazy val parsed: Map[Int, WktCS] = records.mapValues(WKTParser.apply)
  lazy val projections: Set[WktCS] = parsed.values.toSet
  lazy val records: Map[Int, String] = parseWktEpsgResource

  def parseWktEpsgResource(): Map[Int, String] = {
    // read input from epsg.properties file
    val EpsgRx = """(\d+)\=(.*)""".r
    WKT.withWktFile { lines =>
      val iter =
        for {
          line <- lines if ! line.startsWith("#")
          m <- EpsgRx.findFirstMatchIn(line)
        } yield {
          val code = m.group(1).toInt
          val wkt = m.group(2)
          (code, wkt)
        }
      iter.toMap
    }
  }

  def contains(input: WktCS): Boolean = projections contains input

  /**
    * Returns an EPSG code given a WKT string
    * @param wktString
    * @return
    */
  def getEpsgCode(wktString: String): Option[Int] = {
    WKTParser.parse(wktString).toOption.flatMap { wktParsed =>
      val db = parsed.find { case (_, wkt) => wkt == wktParsed }.map(_._1)
      if (db.nonEmpty) db
      else
        wktParsed match {
          case wkt: ProjCS =>
            wkt.extension.flatMap {
              case ExtensionProj4(proj4String) =>
                CRS.fromString(proj4String).epsgCode
              case _ => None
            }
          case _ => None
        }
    }
  }

  /**
    * Returns an EPSG code as a string of the form "EPSG:[number]" given a WKT String
    * @param wktString
    * @return
    */
  def getEpsgStringCode(wktString: String): Option[String] =
    getEpsgCode(wktString).map(c => s"EPSG:${c.toString}")

  /**
    * Returns the WKT string for the given EPSG code
    * @param input The EPSG code, e.g. 4326
    * @return
    */
  def fromEpsgCode(input: Int): Option[String] =
    records.get(input)

  def withWktFile[T](f: Iterator[String] => T): T = {
    val stream = getClass.getResourceAsStream(wktResourcePath)
    try {
      val lines = Source.fromInputStream(stream).getLines()
      f(lines)
    } finally {
      stream.close()
    }
  }
}
