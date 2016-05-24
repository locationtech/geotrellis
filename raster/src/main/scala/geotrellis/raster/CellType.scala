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

import scala.util.matching.Regex
import java.awt.image.DataBuffer
import java.lang.IllegalArgumentException

// Note: CellType defined in package object as
// `type CellType = DataType with NoDataHandling`


/**
  * The [[DataType]] type.
  */
sealed abstract class DataType extends Serializable { self: CellType =>
  val bits: Int
  val isFloatingPoint: Boolean
  val name: String

  def bytes = bits / 8

  /**
    * Union only checks to see that the correct bitsize and int vs
    * floating point values are set.  We can be sure that its
    * operations are safe because all data is converted up to int or
    * double where the internal GT "constant" nodata values are used
    * for *all* tile types. So, for instance, if a [[UserDefinedNoData]]
    * tile is converted to a different UserDefinedNoData tile, the
    * user defined NoData value will be converted to
    * Int.MinValue/Double.NaN during operations and then converted
    * once more from that value down to the second UserDefinedNoData
    * value.
    *
    * @param   other  The other cell type
    * @return         The union of this data type and the other cell type
    */
  def union(other: CellType) =
    if (bits < other.bits)
      other
    else if (bits > other.bits)
      self
    else if (isFloatingPoint && !other.isFloatingPoint)
      self
    else
      other

  /**
    * Compute the intersection of the present data type and the given
    * cell type.
    *
    * @param   other  The other cell type
    * @return         The intersection of this data type and the other cell type
    */
  def intersect(other: CellType) =
    if (bits < other.bits)
      self
    else if (bits > other.bits)
      other
    else if (isFloatingPoint && !other.isFloatingPoint)
      other
    else
      self

  /**
    * Answer true if the present data type contains the other cell
    * type, otherwise false.
    *
    * @param   other  The other cell type
    * @return         True for containment, false otherwise
    */
  def contains(other: CellType) = bits >= other.bits

  /**
    * Return the number of bytes that would be consumed by the given number of items of the present type.
    *
    * @param   size  The number of items
    * @return        The number of bytes
    */
  def numBytes(size: Int) = bytes * size

  /**
    * Return the string representation of this data type.
    *
    * @return  The string representation
    */
  override def toString: String = name
}

/**
  * The [[BitCells]] type, derived from [[DataType]]
  */
sealed trait BitCells extends DataType { self: CellType =>
  val bits: Int = 1
  val isFloatingPoint: Boolean = false
  val name = "bool"
}

/**
  * The [[ByteCells]] type, derived from [[DataType]]
  */
sealed trait ByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  val name = "int8"
}

/**
  * The [[UByteCells]] type, derived from [[DataType]]
  */
sealed trait UByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  val name = "uint8"
}

/**
  * The [[ShortCells]] type, derived from [[DataType]]
  */
sealed trait ShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  val name = "int16"
}

/**
  * The [[UShortCells]] type, derived from [[DataType]]
  */
sealed trait UShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  val name = "uint16"
}

/**
  * The [[IntCells]] type, derived from [[DataType]]
  */
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

/**
  * The [[DoubleCells]] type, derived from [[DataType]]
  */
sealed trait DoubleCells extends DataType { self: CellType =>
  val bits: Int = 64
  val isFloatingPoint: Boolean = true
  val name = "float64"
}

/**
  * The [[NoDataHandling]].
  */
sealed trait NoDataHandling { cellType: CellType => }

/**
  * The [[ConstantNoData]] type, derived from [[NoDataHandling]].
  */
sealed trait ConstantNoData extends NoDataHandling { cellType: CellType => }

/**
  * The [[NoNoData]] type, derived from [[NoDataHandling]].
  */
sealed trait NoNoData extends NoDataHandling { cellType: CellType =>
  abstract override def toString: String = cellType.name + "raw"
}

/**
  * The [[UserDefinedNoData]] type, derived from [[NoDataHandling]].
  */
sealed trait UserDefinedNoData[@specialized(Byte, Short, Int) T] extends NoDataHandling { cellType: CellType =>
  val noDataValue: T
  abstract override def toString: String = cellType.name + "ud" + noDataValue.toString
}

/**
  * The [[BitCellType]] type, derived from [[BitCells]] and
  * [[NoNoData]].
  */
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

case object IntCellType
    extends IntCells with NoNoData
case object IntConstantNoDataCellType
    extends IntCells with ConstantNoData
case class IntUserDefinedNoDataCellType(noDataValue: Int)
    extends IntCells with UserDefinedNoData[Int]

case object FloatCellType
    extends FloatCells with NoNoData
case object FloatConstantNoDataCellType
    extends FloatCells with ConstantNoData
case class FloatUserDefinedNoDataCellType(noDataValue: Float)
    extends FloatCells with UserDefinedNoData[Float]

case object DoubleCellType
    extends DoubleCells with NoNoData
case object DoubleConstantNoDataCellType
    extends DoubleCells with ConstantNoData
case class DoubleUserDefinedNoDataCellType(noDataValue: Double)
    extends DoubleCells with UserDefinedNoData[Double]


// No NoData
object CellType {

  /**
    * Translate an integer representing a cell type into a
    * [[CellType]].  This is the opposite of toAwtType.
    *
    * @param   awtType  An integer representing a cell type
    * @return           The CellType corresponding to awtType
    */
  def fromAwtType(awtType: Int): CellType = awtType match {
    case DataBuffer.TYPE_BYTE => UByteConstantNoDataCellType
    case DataBuffer.TYPE_SHORT => ShortConstantNoDataCellType
    case DataBuffer.TYPE_INT => IntConstantNoDataCellType
    case DataBuffer.TYPE_FLOAT => FloatConstantNoDataCellType
    case DataBuffer.TYPE_DOUBLE => DoubleConstantNoDataCellType
    case _ => throw new IllegalArgumentException(s"AWT type $awtType is not supported")
  }

  /**
    * Translate a string representing a cell type into a [[CellType]].
    *
    * @param   name  An integer representing a cell type, e.g. "uint32"
    * @return        The CellType corresponding to name
    */
  def fromString(name: String): CellType = name match {
    case "bool" | "boolraw" => BitCellType  // No NoData values
    case "int8raw" => ByteCellType
    case "uint8raw" => UByteCellType
    case "int16raw" => ShortCellType
    case "uint16raw" => UShortCellType
    case "float32raw" => FloatCellType
    case "float64raw" => DoubleCellType
    case "int8" => ByteConstantNoDataCellType  // Constant NoData values
    case "uint8" => UByteConstantNoDataCellType
    case "int16" => ShortConstantNoDataCellType
    case "uint16" => UShortConstantNoDataCellType
    case "int32" => IntConstantNoDataCellType
    case "int32raw" => IntCellType
    case "float32" => FloatConstantNoDataCellType
    case "float64" => DoubleConstantNoDataCellType
    case ct if ct.startsWith("int8ud") =>
      val ndVal = new Regex("\\d+$").findFirstIn(ct).getOrElse {
        throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
      ByteUserDefinedNoDataCellType(ndVal.toByte)
    case ct if ct.startsWith("uint8ud") =>
      val ndVal = new Regex("\\d+$").findFirstIn(ct).getOrElse {
        throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
      UByteUserDefinedNoDataCellType(ndVal.toByte)
    case ct if ct.startsWith("int16ud") =>
      val ndVal = new Regex("\\d+$").findFirstIn(ct).getOrElse {
        throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
      ShortUserDefinedNoDataCellType(ndVal.toShort)
    case ct if ct.startsWith("uint16ud") =>
      val ndVal = new Regex("\\d+$").findFirstIn(ct).getOrElse {
        throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
      UShortUserDefinedNoDataCellType(ndVal.toShort)
    case ct if ct.startsWith("int32ud") =>
      val ndVal = new Regex("\\d+$").findFirstIn(ct).getOrElse {
        throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
      IntUserDefinedNoDataCellType(ndVal.toInt)
    case ct if ct.startsWith("float32ud") =>
      try {
        val ndVal = ct.stripPrefix("float32ud").toDouble.toFloat
        if (ndVal.isNaN) FloatConstantNoDataCellType
        else FloatUserDefinedNoDataCellType(ndVal)
      } catch {
        case e: NumberFormatException => throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
    case ct if ct.startsWith("float64ud") =>
      try {
        val ndVal = ct.stripPrefix("float64ud").toDouble
        if (ndVal.isNaN) DoubleConstantNoDataCellType
        else DoubleUserDefinedNoDataCellType(ndVal)
      } catch {
        case e: NumberFormatException => throw new IllegalArgumentException(s"Cell type $name is not supported")
      }
    case str =>
      throw new IllegalArgumentException(s"Cell type $name is not supported")
  }

  /**
    * Translate a [[CellType]] into the corresponding integer
    * representation.  This is the opposite of fromAwtType.
    *
    * @param   cellType  A CellType
    * @return            The corresponding integer representation of the given cell type
    */
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
