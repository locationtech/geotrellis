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
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class UByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Byte)
    extends UByteGeoTiffSegment(bytes)
    with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  def getInt(i: Int): Int = udub2i(getRaw(i))
  def getDouble(i: Int): Double = udub2d(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToUByteOut(v: Double): Byte = d2udb(v)
}
