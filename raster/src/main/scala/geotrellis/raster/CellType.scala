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

// DataType ADT
sealed abstract class DataType extends Serializable { self: CellType =>
  val bits: Int
  val isFloatingPoint: Boolean
  val name: String

  def bytes = bits / 8
  def union(other: CellType) =
    if (bits < other.bits)
      other
    else if (bits < other.bits)
      self
    else if (isFloatingPoint && !other.isFloatingPoint)
      self
    else
      other

  def intersect(other: CellType) =
    if (bits < other.bits)
      self
    else if (bits < other.bits)
      other
    else if (isFloatingPoint && !other.isFloatingPoint)
      other
    else
      self

  def contains(other: CellType) = bits >= other.bits

  def numBytes(size: Int) = bytes * size

  override def toString: String = name
}

sealed trait BitCells extends DataType { self: CellType =>
  val bits: Int = 1
  val isFloatingPoint: Boolean = false
  val name = "bool"
}
sealed trait ByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  val name = "int8"
}
sealed trait UByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  val name = "int8"
}
sealed trait ShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  val name = "int16"
}
sealed trait UShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  val name = "uint16"
}
sealed trait IntCells extends DataType { self: CellType =>
  val bits: Int = 32
  val isFloatingPoint: Boolean = false
  val name = "int32"
}
sealed trait FloatCells extends DataType { self: CellType =>
  val bits: Int = 32
  val isFloatingPoint: Boolean = true
  val name = "float32"
}
sealed trait DoubleCells extends DataType { self: CellType =>
  val bits: Int = 64
  val isFloatingPoint: Boolean = true
  val name = "float64"
}

// NoData ADT
sealed trait NoDataHandling {
  val name: String
}
sealed trait ConstantNoData extends NoDataHandling { dataType: DataType => }
sealed trait NoNoData extends NoDataHandling { dataType: DataType =>
  abstract override val name: String = dataType.name + "raw"
}
sealed trait UserDefinedNoData[@specialized(Byte, Short, Int) T] extends NoDataHandling { dataType: DataType =>
  val noDataValue: T
  abstract override val name: String = dataType.name + "ud" + noDataValue.toString
}

case object BitCellType extends BitCells with NoNoData {
  override final def numBytes(size: Int) = (size + 7) / 8
}

case object ByteCellType
    extends ByteCells with NoNoData
case object ByteConstantNoDataCellType
    extends ByteCells with ConstantNoData
case class ByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends ByteCells with UserDefinedNoData[Byte]

case object UByteCellType
    extends UByteCells with NoNoData
case object UByteConstantNoDataCellType
    extends UByteCells with ConstantNoData
case class UByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends UByteCells with UserDefinedNoData[Byte]

case object ShortCellType
    extends ShortCells with NoNoData
case object ShortConstantNoDataCellType
    extends ShortCells with ConstantNoData
case class ShortUserDefinedNoDataCellType(noDataValue: Short)
    extends ShortCells with UserDefinedNoData[Short]

case object UShortCellType
    extends UShortCells with NoNoData
case object UShortConstantNoDataCellType
    extends UShortCells with ConstantNoData
case class UShortUserDefinedNoDataCellType(noDataValue: Short)
    extends UShortCells with UserDefinedNoData[Short]

case object IntConstantNoDataCellType
    extends IntCells with ConstantNoData

case object FloatConstantNoDataCellType
    extends FloatCells with ConstantNoData

case object DoubleConstantNoDataCellType
    extends DoubleCells with ConstantNoData


// No NoData
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
    case _: BitCells => DataBuffer.TYPE_BYTE
    case _: ByteCells => DataBuffer.TYPE_BYTE
    case _: UByteCells => DataBuffer.TYPE_BYTE
    case _: ShortCells => DataBuffer.TYPE_SHORT
    case _: UShortCells => DataBuffer.TYPE_SHORT
    case _: IntCells => DataBuffer.TYPE_INT
    case _: FloatCells => DataBuffer.TYPE_FLOAT
    case _: DoubleCells => DataBuffer.TYPE_DOUBLE
  }
}
