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

package geotrellis.geowave.index.field

import java.nio.ByteBuffer

import geotrellis.geowave.index.dimension.{Elevation, ElevationDefinition, ElevationReader, ElevationWriter}
import org.locationtech.geowave.core.index.dimension.NumericDimensionDefinition
import org.locationtech.geowave.core.index.dimension.bin.BinRange
import org.locationtech.geowave.core.index.persist.PersistenceUtils
import org.locationtech.geowave.core.index.sfc.data.{NumericData, NumericRange}
import org.locationtech.geowave.core.index.{ByteArrayUtils, StringUtils, VarintUtils}
import org.locationtech.geowave.core.store.data.field.{FieldReader, FieldWriter}
import org.locationtech.geowave.core.store.dimension.NumericDimensionField

class ElevationField(
  private var reader: FieldReader[Elevation] = new ElevationReader(),
  private var writer: FieldWriter[AnyRef, Elevation] = new ElevationWriter(),
  private var fieldName: String = ElevationField.DEFAULT_FIELD_ID,
  private var baseDefinition: NumericDimensionDefinition = new ElevationDefinition(0, 32000)
) extends NumericDimensionField[Elevation] {

  def getNumericData(dataElement: Elevation): NumericData = dataElement.toNumericData

  def getFieldName: String = fieldName

  def getWriter: FieldWriter[_, Elevation] = writer

  def getReader: FieldReader[Elevation] = reader

  def getBaseDefinition: NumericDimensionDefinition = baseDefinition

  def getRange: Double = baseDefinition.getRange

  def normalize(value: Double): Double = baseDefinition.normalize(value)

  def denormalize(value: Double): Double = baseDefinition.denormalize(value)

  def getNormalizedRanges(range: NumericData): Array[BinRange] = baseDefinition.getNormalizedRanges(range)

  def getDenormalizedRange(range: BinRange): NumericRange = baseDefinition.getDenormalizedRange(range)

  def getFixedBinIdSize: Int = baseDefinition.getFixedBinIdSize

  def getBounds: NumericRange = baseDefinition.getBounds

  def getFullRange: NumericData = new NumericRange(0, Int.MaxValue)

  def toBinary: Array[Byte] = {
    val dimensionBinary = PersistenceUtils.toBinary(baseDefinition)
    val fieldNameBytes = StringUtils.stringToBinary(fieldName)
    val buf =
      ByteBuffer.allocate(
        dimensionBinary.length
          + fieldNameBytes.length
          + VarintUtils.unsignedIntByteLength(fieldNameBytes.length))
    VarintUtils.writeUnsignedInt(fieldNameBytes.length, buf)
    buf.put(fieldNameBytes)
    buf.put(dimensionBinary)
    buf.array()
  }

  def fromBinary(bytes: Array[Byte]): Unit = {
    val buf = ByteBuffer.wrap(bytes)
    val fieldNameLength = VarintUtils.readUnsignedInt(buf)
    val fieldNameBinary = ByteArrayUtils.safeRead(buf, fieldNameLength)
    fieldName = StringUtils.stringFromBinary(fieldNameBinary)

    val dimensionBinary = Array.ofDim[Byte](buf.remaining)
    buf.get(dimensionBinary)

    baseDefinition = PersistenceUtils.fromBinary(dimensionBinary).asInstanceOf[NumericDimensionDefinition]
  }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = (prime * result) + (if (baseDefinition == null) 0 else baseDefinition.hashCode)
    result = (prime * result) + (if (fieldName == null) 0 else fieldName.hashCode)
    result
  }

  override def equals(obj: Any): Boolean = {
    if (this eq obj.asInstanceOf[AnyRef]) return true
    if (obj == null) return false
    if (getClass ne obj.getClass) return false
    val other = obj.asInstanceOf[ElevationField]
    if (baseDefinition == null) if (other.baseDefinition != null) return false
    else if (!(baseDefinition == other.baseDefinition)) return false
    if (fieldName == null) if (other.fieldName != null) return false
    else if (!(fieldName == other.fieldName)) return false
    true
  }
}

object ElevationField {
  val DEFAULT_FIELD_ID = "default_elevation_dimension"

  def apply(): ElevationField = new ElevationField()
  def apply(maxValue: Double): ElevationField = new ElevationField(baseDefinition = new ElevationDefinition(0, maxValue.toInt))
  def apply(maxValue: Int): ElevationField = new ElevationField(baseDefinition = new ElevationDefinition(0, maxValue))
}
