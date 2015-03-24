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

package geotrellis.vector.io

import com.vividsolutions.jts.io.{WKBReader}
import com.vividsolutions.jts.{geom => jts}
import geotrellis.vector._

/**
 * Wraps WKB Writer and Reader, thread safe
 */
object WKB {
  private val readerBox = new ThreadLocal[WKBReader]
  private val writerBox = new ThreadLocal[WKBWriter]

  def read[G <: Geometry](value: Array[Byte]): G = {
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry[G](readerBox.get.read(value))
  }

  def read[G <: Geometry](hex: String): G = {
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry[G](readerBox.get.read(WKBReader.hexToBytes(hex)))
  }

  def write(geom: Geometry, srid: Int = 0): Array[Byte] = {
    if (writerBox.get == null) writerBox.set(new WKBWriter(2))
    if (srid == 0)
      writerBox.get.write(geom)
    else
      writerBox.get.write(geom, Some(srid))
  }
}
