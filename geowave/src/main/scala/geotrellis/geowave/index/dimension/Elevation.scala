/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.index.dimension

import java.nio.ByteBuffer

import geotrellis.geowave.utils.DoubleUtils
import org.locationtech.geowave.core.index.persist.Persistable
import org.locationtech.geowave.core.index.sfc.data.{NumericData, NumericRange}
import org.locationtech.geowave.core.index.{ByteArrayUtils, VarintUtils}
import org.locationtech.geowave.core.store.dimension.NumericDimensionField
import org.locationtech.geowave.core.store.index.CommonIndexValue

/** Elevation common index value.
  * @param minValue minimum elevation included in this index value, inclusive
  * @param maxValue maximum elevation included in this index value, inclusive
  */
class Elevation(
  private var minValue: Double = 0d,
  private var maxValue: Double = 0d
) extends CommonIndexValue with Persistable {

  private var visibility: Array[Byte] = Array.empty
  def getMinValue: Double = minValue
  def getMaxValue: Double = maxValue
  def getLength: Double = maxValue - minValue

  def toBinary: Array[Byte] = {
    val bytes =
      ByteBuffer.allocate(
        DoubleUtils.doubleByteLength
          + DoubleUtils.doubleByteLength
          + VarintUtils.unsignedIntByteLength(visibility.length)
          + visibility.length
      )

    DoubleUtils.writeDouble(minValue, bytes)
    DoubleUtils.writeDouble(maxValue, bytes)
    VarintUtils.writeUnsignedInt(visibility.length, bytes)
    bytes.put(visibility)

    bytes.array()
  }

  def fromBinary(bytes: Array[Byte]): Unit = {
    val buf = ByteBuffer.wrap(bytes)

    minValue   = DoubleUtils.readDouble(buf)
    maxValue   = DoubleUtils.readDouble(buf)
    val length = VarintUtils.readUnsignedInt(buf)
    visibility = ByteArrayUtils.safeRead(buf, length)
  }

  def getVisibility: Array[Byte] = visibility

  def setVisibility(bytes: Array[Byte]): Unit = visibility = bytes

  def overlaps(field: Array[NumericDimensionField[_ <: CommonIndexValue]], rangeData: Array[NumericData]): Boolean = {
    val nd = rangeData(0)
    ((nd.getMin >= getMinValue) && (nd.getMax <= getMaxValue)) ||
    ((nd.getMin <= getMinValue) && (nd.getMax >= getMinValue)) ||
    ((nd.getMin <= getMaxValue) && (nd.getMax >= getMaxValue))
  }

  def toNumericData: NumericData = new NumericRange(minValue, maxValue)

  override def toString: String = s"Elevation($minValue, $maxValue)"
}

object Elevation {
  val DEFAULT_FIELD_NAME = "default_elevation_dimension"
  def apply(minValue: Double, maxValue: Double): Elevation = new Elevation(minValue, maxValue)
  def apply(value: Double): Elevation = new Elevation(value, value)
  def apply(): Elevation = new Elevation()
}
