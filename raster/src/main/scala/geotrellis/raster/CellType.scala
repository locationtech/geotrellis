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

package geotrellis.raster

import java.awt.image.DataBuffer

sealed abstract class CellType(val bits: Int, val name: String, val isFloatingPoint: Boolean) extends Serializable {
  def bytes = bits / 8
  def union(other: CellType) = 
    if (bits < other.bits) 
      other
    else if (bits < other.bits)
      this
    else if (isFloatingPoint && !other.isFloatingPoint)
      this
    else
      other

  def intersect(other: CellType) = 
    if (bits < other.bits)
      this
    else if (bits < other.bits)
      other
    else if (isFloatingPoint && !other.isFloatingPoint)
      other
    else
      this

  def contains(other: CellType) = bits >= other.bits

  def numBytes(size: Int) = bytes * size

  override def toString: String = name
}
sealed abstract class DynamicCellType(bits: Int, name: String, isFloatingPoint: Boolean, val noDataValue: Double) extends CellType(bits, name, isFloatingPoint)

object DynamicCellType {
  def unapply(dct: DynamicCellType): Option[(Int, String, Boolean, Double)] = Option(dct) map { dct =>
    (dct.bits, dct.name, dct.isFloatingPoint, dct.noDataValue)
  }
}

sealed abstract class OptimizedCellType(bits: Int, name: String, isFloatingPoint: Boolean) extends CellType(bits, name, isFloatingPoint)
sealed abstract class RawCellType(bits: Int, name: String, isFloatingPoint: Boolean) extends OptimizedCellType(bits, name, isFloatingPoint)
sealed abstract class NoDataCellType(bits: Int, name: String, isFloatingPoint: Boolean) extends OptimizedCellType(bits, name, isFloatingPoint)

case object TypeRawByte   extends RawCellType(8, "int8raw", false)
case object TypeRawUByte  extends RawCellType(8, "uint8raw", false)
case object TypeRawShort  extends RawCellType(16, "int16raw", false)
case object TypeRawUShort extends RawCellType(16, "uint16raw", false)

case object TypeBit    extends NoDataCellType(1, "bool", false) {
  override final def numBytes(size: Int) = (size + 7) / 8
}
case object TypeByte   extends NoDataCellType(8, "int8", false)
case object TypeUByte  extends NoDataCellType(8, "uint8", false)
case object TypeShort  extends NoDataCellType(16, "int16", false)
case object TypeUShort extends NoDataCellType(16, "uint16", false)
case object TypeInt    extends NoDataCellType(32, "int32", false)
case object TypeUInt    extends NoDataCellType(32, "uint32", true) // We cast to float for its greater range at the cost of some accuracy
case object TypeFloat  extends NoDataCellType(32, "float32", true)
case object TypeDouble extends NoDataCellType(64, "float64", true)

case class TypeDynamicByte(nd: Double)   extends DynamicCellType(8, "int8dynamic", false, nd)
case class TypeDynamicUByte(nd: Double)  extends DynamicCellType(8, "uint8dynamic", false, nd)
case class TypeDynamicShort(nd: Double)  extends DynamicCellType(16, "int16dynamic", false, nd)
case class TypeDynamicUShort(nd: Double) extends DynamicCellType(16, "uint16dynamic", false, nd)

object CellType {
  def fromAwtType(awtType: Int): CellType = awtType match {
    case DataBuffer.TYPE_BYTE   => TypeByte
    case DataBuffer.TYPE_DOUBLE => TypeDouble
    case DataBuffer.TYPE_FLOAT  => TypeFloat
    case DataBuffer.TYPE_INT    => TypeInt
    case DataBuffer.TYPE_SHORT  => TypeShort
    case _                      => sys.error(s"Cell type with AWT type $awtType is not supported")
  }

  def fromString(name: String): CellType = name match {
    case "bool"       => TypeBit
    case "int8"       => TypeByte
    case "int8raw"    => TypeRawByte
    case "uint8"      => TypeUByte
    case "uint8raw"   => TypeRawUByte
    case "int16"      => TypeShort
    case "int16raw"   => TypeRawShort
    case "uint16"     => TypeUShort
    case "uint16raw"  => TypeRawUShort
    case "int32"      => TypeInt
    case "float32"    => TypeFloat
    case "float64"    => TypeDouble
    case _ => sys.error(s"Cell type $name is not supported")
  }

  def toAwtType(cellType: CellType): Int = cellType match {
    case TypeBit                                           => DataBuffer.TYPE_BYTE
    case TypeByte | TypeRawByte | TypeDynamicByte(_)       => DataBuffer.TYPE_BYTE
    case TypeUByte | TypeRawUByte | TypeDynamicUByte(_)    => DataBuffer.TYPE_SHORT
    case TypeShort | TypeRawShort | TypeDynamicShort(_)    => DataBuffer.TYPE_SHORT
    case TypeUShort | TypeRawUShort | TypeDynamicUShort(_) => DataBuffer.TYPE_INT
    case TypeInt                                           => DataBuffer.TYPE_INT
    case TypeFloat                                         => DataBuffer.TYPE_FLOAT
    case TypeDouble                                        => DataBuffer.TYPE_DOUBLE
  }
}
