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

package geotrellis.raster

import java.awt.image.DataBuffer

// Note: CellType defined in package object as
// `type CellType = DataType with NoDataHandling`

/**
  * The [[DataType]] type.
  */
sealed abstract class DataType extends Serializable { self: CellType =>
  val bits: Int
  val isFloatingPoint: Boolean
  def name: String = CellType.toName(self)

  /** Determine if two [[CellType]] instances have equal [[DataType]] component */
  def equalDataType(other: DataType): Boolean

  /** Creates CellType with requested NoData semantics.
    * In case where [[DataType]] is not Double noDataValue will be coerced to that type.
    * This may lead to loss of precision but will leave NoData consistent with tile cells.
    *
    * @param noDataValue Optional NoData Value
    * @return [[DataType]] unchanged but with [[NoDataHandling]] implied by the value of the parameter
    */
  def withNoData(noDataValue: Option[Double]): CellType

  /**
   * Creates a [[CellType]] with the default `ConstantNoData` value. If instance is already one of the default `NoData`
   * values then a reference to self is returned.
   * @return [[CellType]] with same bit width as this but with the default `NoData` value.
   */
  def withDefaultNoData(): CellType

  /**
   * Bytes per sample (bits divided by 8).
   *
   * @return Bytes per sample.
   */
  def bytes: Int = bits / 8

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
  def union(other: CellType): CellType =
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
  def intersect(other: CellType): CellType =
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
  def contains(other: CellType): Boolean = bits >= other.bits

  /**
    * Return the number of bytes that would be consumed by the given number of items of the present type.
    *
    * @param   size  The number of items
    * @return        The number of bytes
    */
  def numBytes(size: Int): Int = bytes * size

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
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[BitCells]
  def withNoData(noDataValue: Option[Double]): BitCells with NoDataHandling =
    BitCellType // No other options is possible
  def withDefaultNoData(): BitCells with NoDataHandling = BitCellType
}

/**
  * The [[ByteCells]] type, derived from [[DataType]]
  */
sealed trait ByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[ByteCells]
  def withNoData(noDataValue: Option[Double]): ByteCells with NoDataHandling =
    ByteCells.withNoData(noDataValue.map(_.toByte))
  def withDefaultNoData(): ByteCells with NoDataHandling = ByteConstantNoDataCellType
}

object ByteCells {
  def withNoData(noDataValue: Option[Byte]): ByteCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd == Byte.MinValue =>
        ByteConstantNoDataCellType
      case Some(nd) =>
        ByteUserDefinedNoDataCellType(nd)
      case None =>
        ByteCellType
    }
}

/**
  * The [[UByteCells]] type, derived from [[DataType]]
  */
sealed trait UByteCells extends DataType { self: CellType =>
  val bits: Int = 8
  val isFloatingPoint: Boolean = false
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[UByteCells]
  def withNoData(noDataValue: Option[Double]): UByteCells with NoDataHandling =
    UByteCells.withNoData(noDataValue.map(_.toByte))
  def withDefaultNoData(): UByteCells with NoDataHandling = UByteConstantNoDataCellType
}

object UByteCells {
  def withNoData(noDataValue: Option[Byte]): UByteCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd == 0 =>
        UByteConstantNoDataCellType
      case Some(nd) =>
        UByteUserDefinedNoDataCellType(nd)
      case None =>
        UByteCellType
    }
}

/**
  * The [[ShortCells]] type, derived from [[DataType]]
  */
sealed trait ShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[ShortCells]
  def withNoData(noDataValue: Option[Double]): ShortCells with NoDataHandling =
    ShortCells.withNoData(noDataValue.map(_.toShort))
  def withDefaultNoData(): ShortCells with NoDataHandling = ShortConstantNoDataCellType
}

object ShortCells {
  def withNoData(noDataValue: Option[Short]): ShortCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd == Short.MinValue =>
        ShortConstantNoDataCellType
      case Some(nd) =>
        ShortUserDefinedNoDataCellType(nd)
      case None =>
        ShortCellType
    }
}

/**
  * The [[UShortCells]] type, derived from [[DataType]]
  */
sealed trait UShortCells extends DataType { self: CellType =>
  val bits: Int = 16
  val isFloatingPoint: Boolean = false
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[UShortCells]
  def withNoData(noDataValue: Option[Double]): UShortCells with NoDataHandling =
    UShortCells.withNoData(noDataValue.map(_.toShort))
  def withDefaultNoData(): UShortCells with NoDataHandling = UShortConstantNoDataCellType
}

object UShortCells {
  def withNoData(noDataValue: Option[Short]): UShortCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd == 0 =>
        UShortConstantNoDataCellType
      case Some(nd) =>
        UShortUserDefinedNoDataCellType(nd)
      case None =>
        UShortCellType
    }
}

/**
  * The [[IntCells]] type, derived from [[DataType]]
  */
sealed trait IntCells extends DataType { self: CellType =>
  val bits: Int = 32
  val isFloatingPoint: Boolean = false
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[IntCells]
  def withNoData(noDataValue: Option[Double]): IntCells with NoDataHandling =
    IntCells.withNoData(noDataValue.map(_.toInt))
  def withDefaultNoData(): IntCells with NoDataHandling = IntConstantNoDataCellType
}

object IntCells {
  def withNoData(noDataValue: Option[Int]): IntCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd == Int.MinValue =>
        IntConstantNoDataCellType
      case Some(nd) =>
        IntUserDefinedNoDataCellType(nd)
      case None =>
        IntCellType
    }
}

sealed trait FloatCells extends DataType { self: CellType =>
  val bits: Int = 32
  val isFloatingPoint: Boolean = true
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[FloatCells]
  def withNoData(noDataValue: Option[Double]): FloatCells with NoDataHandling =
    FloatCells.withNoData(noDataValue.map(_.toFloat))
  def withDefaultNoData(): FloatCells with NoDataHandling = FloatConstantNoDataCellType
}

object FloatCells {
  def withNoData(noDataValue: Option[Float]): FloatCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd.isNaN =>
        FloatConstantNoDataCellType
      case Some(nd) =>
        FloatUserDefinedNoDataCellType(nd)
      case None =>
        FloatCellType
    }
}

/**
  * The [[DoubleCells]] type, derived from [[DataType]]
  */
sealed trait DoubleCells extends DataType { self: CellType =>
  val bits: Int = 64
  val isFloatingPoint: Boolean = true
  def equalDataType(other: DataType): Boolean = other.isInstanceOf[DoubleCells]
  def withNoData(noDataValue: Option[Double]): DoubleCells with NoDataHandling =
    DoubleCells.withNoData(noDataValue)
  def withDefaultNoData(): DoubleCells with NoDataHandling = DoubleConstantNoDataCellType

}

object DoubleCells {
  def withNoData(noDataValue: Option[Double]): DoubleCells with NoDataHandling =
    noDataValue match {
      case Some(nd) if nd.isNaN =>
        DoubleConstantNoDataCellType
      case Some(nd) =>
        DoubleUserDefinedNoDataCellType(nd)
      case None =>
        DoubleCellType
    }
}

/**
  * Base trait for all cell types associated with handling `NoData` values or not.
  */
sealed trait NoDataHandling { cellType: CellType => }

/**
 * Base trait for all "raw" cell types, not having a `NoData` value.
 */
sealed trait NoNoData extends NoDataHandling { cellType: CellType => }

/**
 *  Base trait for all cell type having a `NoData` value
 */
sealed trait HasNoData[@specialized(Byte, Short, Int, Float, Double) T] extends NoDataHandling { cellType: CellType =>
  /** The no data value as represented in the JVM in the underlying cell. If unsigned types are involved then
   * this value may be an overflow representation, and  `widenedNoData` should be used.*/
  val noDataValue: T

  def widenedNoData(implicit ev: Numeric[T]): WidenedNoData =
    if (cellType.isFloatingPoint) WideDoubleNoData(ev.toDouble(noDataValue))
    else WideIntNoData(ev.toInt(noDataValue))
}

/**
  * The [[ConstantNoData]] type, derived from [[NoDataHandling]].
  */
sealed trait ConstantNoData[@specialized(Byte, Short, Int, Float, Double) T] extends HasNoData[T] { cellType: CellType => }

/**
  * The [[UserDefinedNoData]] type, derived from [[NoDataHandling]].
  */
sealed trait UserDefinedNoData[@specialized(Byte, Short, Int, Float, Double) T]
  extends HasNoData[T] { cellType: CellType => }

/**
  * The [[BitCellType]] type, derived from [[BitCells]] and
  * [[NoNoData]].
  */
case object BitCellType extends BitCells with NoNoData {
  override final def numBytes(size: Int): Int = (size + 7) / 8
}

case object ByteCellType
    extends ByteCells with NoNoData
case object ByteConstantNoDataCellType
    extends ByteCells with ConstantNoData[Byte] { val noDataValue = byteNODATA }
case class ByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends ByteCells with UserDefinedNoData[Byte]

case object UByteCellType
    extends UByteCells with NoNoData
case object UByteConstantNoDataCellType
    extends UByteCells with ConstantNoData[Byte] { val noDataValue = ubyteNODATA }
case class UByteUserDefinedNoDataCellType(noDataValue: Byte)
    extends UByteCells with UserDefinedNoData[Byte] {
  override def widenedNoData(implicit ev: Numeric[Byte]) = WideIntNoData(noDataValue)
}

case object ShortCellType
    extends ShortCells with NoNoData
case object ShortConstantNoDataCellType
    extends ShortCells with ConstantNoData[Short] { val noDataValue = shortNODATA }
case class ShortUserDefinedNoDataCellType(noDataValue: Short)
    extends ShortCells with UserDefinedNoData[Short]

case object UShortCellType
    extends UShortCells with NoNoData
case object UShortConstantNoDataCellType
    extends UShortCells with ConstantNoData[Short] { val noDataValue = ushortNODATA }
case class UShortUserDefinedNoDataCellType(noDataValue: Short)
    extends UShortCells with UserDefinedNoData[Short] {
  override def widenedNoData(implicit ev: Numeric[Short]) = WideIntNoData(noDataValue)
}

case object IntCellType
    extends IntCells with NoNoData
case object IntConstantNoDataCellType
    extends IntCells with ConstantNoData[Int] { val noDataValue = NODATA }
case class IntUserDefinedNoDataCellType(noDataValue: Int)
    extends IntCells with UserDefinedNoData[Int]

case object FloatCellType
    extends FloatCells with NoNoData
case object FloatConstantNoDataCellType
    extends FloatCells with ConstantNoData[Float] { val noDataValue = floatNODATA }
case class FloatUserDefinedNoDataCellType(noDataValue: Float)
    extends FloatCells with UserDefinedNoData[Float]

case object DoubleCellType
    extends DoubleCells with NoNoData
case object DoubleConstantNoDataCellType
    extends DoubleCells with ConstantNoData[Double] { val noDataValue = doubleNODATA }
case class DoubleUserDefinedNoDataCellType(noDataValue: Double)
    extends DoubleCells with UserDefinedNoData[Double]

object CellType {
  import CellTypeEncoding._

  /**
   * Translate a string representing a cell type into a [[CellType]].
   *
   * @param name A string representing a cell type, as reported by [[DataType.name]] e.g. "uint32"
   * @return The CellType corresponding to `name`
   */
  def fromName(name: String): CellType = {
    name match {
      case bool() | boolraw() => BitCellType // No NoData values
      case int8raw() => ByteCellType
      case uint8raw() => UByteCellType
      case int16raw() => ShortCellType
      case uint16raw() => UShortCellType
      case float32raw() => FloatCellType
      case float64raw() => DoubleCellType
      case int8() => ByteConstantNoDataCellType // Constant NoData values
      case uint8() => UByteConstantNoDataCellType
      case int16() => ShortConstantNoDataCellType
      case uint16() => UShortConstantNoDataCellType
      case int32() => IntConstantNoDataCellType
      case int32raw() => IntCellType
      case float32() => FloatConstantNoDataCellType
      case float64() => DoubleConstantNoDataCellType
      case int8ud(nd) => ByteUserDefinedNoDataCellType(nd.asInt.toByte)
      case uint8ud(nd) => UByteUserDefinedNoDataCellType(nd.asInt.toByte)
      case int16ud(nd) => ShortUserDefinedNoDataCellType(nd.asInt.toShort)
      case uint16ud(nd) => UShortUserDefinedNoDataCellType(nd.asInt.toShort)
      case int32ud(nd) => IntUserDefinedNoDataCellType(nd.asInt)
      case float32ud(nd) =>
        if (nd.asDouble.isNaN) FloatConstantNoDataCellType
        else FloatUserDefinedNoDataCellType(nd.asDouble.toFloat)
      case float64ud(nd) =>
        if (nd.asDouble.isNaN) DoubleConstantNoDataCellType
        else DoubleUserDefinedNoDataCellType(nd.asDouble)
      case _ =>
        throw new IllegalArgumentException(s"Cell type $name is not supported")
    }
  }

  /**
   * Translates a [[CellType]] into its canonical String representation.
   *
   * @param cellType item to convert
   * @return String representation
   */
  def toName(cellType: CellType): String = {

    val encoding = cellType match {
      case BitCellType => bool
      case ByteCellType => int8raw
      case UByteCellType => uint8raw
      case ShortCellType => int16raw
      case UShortCellType => uint16raw
      case IntCellType => int32raw
      case FloatCellType => float32raw
      case DoubleCellType => float64raw
      case ByteConstantNoDataCellType => int8
      case UByteConstantNoDataCellType => uint8
      case ShortConstantNoDataCellType => int16
      case UShortConstantNoDataCellType => uint16
      case IntConstantNoDataCellType => int32
      case FloatConstantNoDataCellType => float32
      case DoubleConstantNoDataCellType => float64
      case ct: ByteUserDefinedNoDataCellType => int8ud(ct.widenedNoData.asInt)
      case ct: UByteUserDefinedNoDataCellType => uint8ud(ct.widenedNoData.asInt)
      case ct: ShortUserDefinedNoDataCellType => int16ud(ct.widenedNoData.asInt)
      case ct: UShortUserDefinedNoDataCellType => uint16ud(ct.widenedNoData.asInt)
      case ct: IntUserDefinedNoDataCellType => int32ud(ct.widenedNoData.asInt)
      case ct: FloatUserDefinedNoDataCellType => float32ud(ct.widenedNoData.asDouble)
      case ct: DoubleUserDefinedNoDataCellType => float64ud(ct.widenedNoData.asDouble)
    }

    encoding.name
  }

  /**
   * Translate an integer representing a cell type into a
   * [[CellType]].  This is the opposite of toAwtType.
   *
   * @param   awtType An integer representing a cell type
   * @return The CellType corresponding to awtType
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
   * Translate a [[CellType]] into the corresponding integer
   * representation.  This is the opposite of fromAwtType.
   *
   * @param   cellType A CellType
   * @return The corresponding integer representation of the given cell type
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

  /** Enumeration of pre-defined cell types without a `NoData` value. */
  val noNoDataCellTypes = Seq(
    BitCellType,
    ByteCellType,
    UByteCellType,
    ShortCellType,
    UShortCellType,
    IntCellType,
    FloatCellType,
    DoubleCellType
  )

  /** Enumeration of pre-defined cell types with a default `NoData` value. */
  val constantNoDataCellTypes = Seq(
    ByteConstantNoDataCellType,
    UByteConstantNoDataCellType,
    ShortConstantNoDataCellType,
    UShortConstantNoDataCellType,
    IntConstantNoDataCellType,
    FloatConstantNoDataCellType,
    DoubleConstantNoDataCellType
  )

  /** Enumeration of all pre-defined cell types. */
  val celltypes: Seq[DataType with NoDataHandling with Product] = noNoDataCellTypes ++ constantNoDataCellTypes
}
