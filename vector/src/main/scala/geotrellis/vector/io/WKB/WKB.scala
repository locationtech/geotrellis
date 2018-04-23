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

package geotrellis.vector.io.wkb

import geotrellis.util.LazyLogging
import geotrellis.vector._

import org.locationtech.jts.io.WKBReader


/** A thread-safe wrapper for the [https://en.wikipedia.org/wiki/Well-known_text#Well-known_binary WKB]
  * Writer and Reader
  */
object WKB extends LazyLogging {
  private val readerBox = new ThreadLocal[WKBReader]
  private val writerBox = new ThreadLocal[WKBWriter]

  /** Convert Well Known Binary to Geometry */
  def read(value: Array[Byte]): Geometry = {
    logger.debug(s"Reading WKB from bytes: ${value.toList}")
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry(readerBox.get.read(value))
  }

  /** Convert Well Known Binary to Geometry */
  def read(hex: String): Geometry = {
    logger.debug(s"Reading WKB from hex: ${hex}")
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry(readerBox.get.read(WKBReader.hexToBytes(hex)))
  }

  /** Convert Geometry to Well Known Binary */
  def write(geom: Geometry, srid: Int = 0): Array[Byte] = {
    if (writerBox.get == null) writerBox.set(new WKBWriter(2))
    if (srid == 0)
      writerBox.get.write(geom)
    else
      writerBox.get.write(geom, Some(srid))
  }
}
