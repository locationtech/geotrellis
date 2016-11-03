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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Int32UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Int)
    extends Int32GeoTiffSegment(bytes)
       with UserDefinedIntNoDataConversions {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = udi2d(get(i))

  protected def intToIntOut(v: Int): Int = i2udi(v)
  protected def doubleToIntOut(v: Double): Int = d2udi(v)
}
