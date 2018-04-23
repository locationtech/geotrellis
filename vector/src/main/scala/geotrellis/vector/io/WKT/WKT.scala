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

package geotrellis.vector.io.wkt

import geotrellis.vector._

import org.locationtech.jts.io.{WKTReader, WKTWriter}
import com.typesafe.scalalogging.LazyLogging

/** A thread-safe wrapper for the WKT Writer and Reader */
object WKT extends LazyLogging {
  private val readerBox = new ThreadLocal[WKTReader]
  private val writerBox = new ThreadLocal[WKTWriter]

  def read(value: String): Geometry = {
    logger.debug(s"Reading WKT from string: ${value}")
    if (readerBox.get == null) readerBox.set(new WKTReader(GeomFactory.factory))
    Geometry(readerBox.get.read(value))
  }

  def write(geom: Geometry): String = {
    if (writerBox.get == null) writerBox.set(new WKTWriter())
    writerBox.get.write(geom.jtsGeom)
  }
}
