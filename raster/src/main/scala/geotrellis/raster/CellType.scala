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
sealed abstract class RawCellType(bits: Int, name: String, isFloatingPoint: Boolean)
    extends CellType(bits, name, isFloatingPoint)
object RawCellType {
  def unapply(rct: RawCellType): Option[(Int, String, Boolean)] =
    Option(rct) map { rct =>
      (rct.bits, rct.name, rct.isFloatingPoint)
    }
}

sealed abstract class ConstantNoDataCellType(bits: Int, name: String, isFloatingPoint: Boolean)
    extends CellType(bits, name, isFloatingPoint)
object ConstantNoDataCellType {
  def unapply(cct: ConstantNoDataCellType): Option[(Int, String, Boolean)] =
    Option(cct) map { ndct =>
      (cct.bits, cct.name, cct.isFloatingPoint)
    }
}

sealed abstract class UserDefinedNoDataCellType[@specialized(Byte, Short, Int) T](bits: Int, name: String, isFloatingPoint: Boolean)
    extends CellType(bits, name, isFloatingPoint) {
  val noDataValue: T
}
object UserDefinedNoDataCellType {
  def unapply[@specialized(Byte, Short, Int) T](udct: UserDefinedNoDataCellType[T]): Option[(Int, String, Boolean, T)] =
    Option(udct) map { dct =>
      (udct.bits, udct.name, udct.isFloatingPoint, udct.noDataValue)
    }
}

// No NoData
case object BitCellType    extends RawCellType(1, "bool", false) {
  override final def numBytes(size: Int) = (size + 7) / 8
}
case object ByteCellType   extends RawCellType(8, "int8raw", false)
case object UByteCellType  extends RawCellType(8, "uint8raw", false)
case object ShortCellType  extends RawCellType(16, "int16raw", false)
case object UShortCellType extends RawCellType(16, "uint16raw", false)

// Constant NoData values (the preferred, standard case in GeoTrellis)
case object ByteConstantNoDataCellType
    extends ConstantNoDataCellType(8, "int8", false)
case object UByteConstantNoDataCellType
    extends ConstantNoDataCellType(8, "uint8", false)
case object ShortConstantNoDataCellType
    extends ConstantNoDataCellType(16, "int16", false)
case object UShortConstantNoDataCellType
    extends ConstantNoDataCellType(16, "uint16", false)
case object IntConstantNoDataCellType
    extends ConstantNoDataCellType(32, "int32", false)
//case object UIntConstantNoDataCellType
//    extends ConstantNoDataCellType(32, "uint32", true) // We cast to float for its greater range at the cost of some accuracy
case object FloatConstantNoDataCellType
    extends ConstantNoDataCellType(32, "float32", true)
case object DoubleConstantNoDataCellType
    extends ConstantNoDataCellType(64, "float64", true)


// User Defined
case class ByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends UserDefinedNoDataCellType[Byte](8, s"int8ud${noDataValue}", false)
case class UByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends UserDefinedNoDataCellType[Byte](8, s"uint8ud${noDataValue}", false)
case class ShortUserDefinedNoDataCellType(noDataValue: Short)
    extends UserDefinedNoDataCellType[Short](16, s"int16ud${noDataValue}", false)
case class UShortUserDefinedNoDataCellType(noDataValue: Short)
    extends UserDefinedNoDataCellType[Short](16, s"uint16ud${noDataValue}", false)

object CellType {
  def fromAwtType(awtType: Int): CellType = awtType match {
    case DataBuffer.TYPE_BYTE => ByteConstantNoDataCellType
    case DataBuffer.TYPE_SHORT => ShortConstantNoDataCellType
    case DataBuffer.TYPE_INT => IntConstantNoDataCellType
    case DataBuffer.TYPE_FLOAT => FloatConstantNoDataCellType
    case DataBuffer.TYPE_DOUBLE => DoubleConstantNoDataCellType
    case _ => sys.error(s"Cell type with AWT type $awtType is not supported")
  }

  def fromString(name: String): CellType = name match {
    case "bool" => BitCellType  // No NoData values
    case "int8" => ByteCellType
    case "uint8" => UByteCellType
    case "int16" => ShortCellType
    case "uint16" => UShortCellType
    case "int8const" => ByteConstantNoDataCellType  // Constant NoData values
    case "uint8const" => UByteConstantNoDataCellType
    case "int16const" => UByteConstantNoDataCellType
    case "uint16const" => ShortConstantNoDataCellType
    case "int32const" => UShortConstantNoDataCellType
    case "float32const" => FloatConstantNoDataCellType
    case "float64const" => DoubleConstantNoDataCellType
    case _ => sys.error(s"Cell type $name is not supported")
  }

  def toAwtType(cellType: CellType): Int = cellType match {
    case BitCellType => DataBuffer.TYPE_BYTE
    case ByteConstantNoDataCellType | ByteCellType | ByteUserDefinedNoDataCellType(_) => DataBuffer.TYPE_BYTE
    case UByteConstantNoDataCellType | UByteCellType | UByteUserDefinedNoDataCellType(_) => DataBuffer.TYPE_SHORT
    case ShortConstantNoDataCellType | ShortCellType | ShortUserDefinedNoDataCellType(_) => DataBuffer.TYPE_SHORT
    case UShortConstantNoDataCellType | UShortCellType | UShortUserDefinedNoDataCellType(_) => DataBuffer.TYPE_INT
    case IntConstantNoDataCellType => DataBuffer.TYPE_INT
    case FloatConstantNoDataCellType => DataBuffer.TYPE_FLOAT
    case DoubleConstantNoDataCellType => DataBuffer.TYPE_DOUBLE
  }
}
