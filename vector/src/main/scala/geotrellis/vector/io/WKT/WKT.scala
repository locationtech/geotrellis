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

package geotrellis.vector.io.wkt

import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import com.vividsolutions.jts.{geom => jts}
import geotrellis.vector._

/**
 * Wraps WKT Writer and Reader, thread safe
 */
object WKT {
  private val readerBox = new ThreadLocal[WKTReader]
  private val writerBox = new ThreadLocal[WKTWriter]

  def read[G <: Geometry](value: String): G = {
    if (readerBox.get == null) readerBox.set(new WKTReader(GeomFactory.factory))
    Geometry[G](readerBox.get.read(value))
  }

  def write(geom: Geometry): String = {
    if (writerBox.get == null) writerBox.set(new WKTWriter())
    writerBox.get.write(geom.jtsGeom)
  }
}
