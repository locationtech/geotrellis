package geotrellis.raster.op

import geotrellis.raster._

abstract class ConvertOp extends Op with Op.Unary {
  def cellType: CellType
  def get(col: Int, row: Int): Int
  def getDouble(col: Int, row: Int): Double
}

object ConvertOp {
  def apply(src: Op, cellType: CellType) =
    cellType match {
      case DoubleCellType =>
        new DoubleRawConvert(src)
      case DoubleConstantNoDataCellType =>
        new DoubleConstantNoDataConvert(src)
      case ct: DoubleUserDefinedNoDataCellType =>
        new DoubleUserDefinedNoDataConvert(src, ct)

      case FloatCellType =>
        new FloatRawConvert(src)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataConvert(src)
      case ct: FloatUserDefinedNoDataCellType =>
        new FloatUserDefinedNoDataConvert(src, ct)

      case IntCellType =>
        new IntRawConvert(src)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataConvert(src)
      case ct: IntUserDefinedNoDataCellType =>
        new IntUserDefinedNoDataConvert(src, ct)

      case UShortCellType =>
        new ShortRawConvert(src)
      case UShortConstantNoDataCellType =>
        new UShortConstantNoDataConvert(src)
      case ct: UShortUserDefinedNoDataCellType =>
        new UShortUserDefinedNoDataConvert(src, ct)

      case ShortCellType =>
        new ShortRawConvert(src)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataConvert(src)
      case ct: ShortUserDefinedNoDataCellType =>
        new ShortUserDefinedNoDataConvert(src, ct)

      case UByteCellType =>
        new UByteRawConvert(src)
      case UByteConstantNoDataCellType =>
        new UByteConstantNoDataConvert(src)
      case ct: UByteUserDefinedNoDataCellType =>
        new UByteUserDefinedNoDataConvert(src, ct)

      case ByteCellType =>
        new ByteRawConvert(src)
      case ByteConstantNoDataCellType =>
        new ByteConstantNoDataConvert(src)
      case ct: ByteUserDefinedNoDataCellType =>
        new ByteUserDefinedNoDataConvert(src, ct.noDataValue)

      case BitCellType =>
        new BitRawConvert(src)
    }
}


// Bit
case class BitRawConvert(src: Op) extends ConvertOp {
  def cellType = BitCellType
  def get(col: Int, row: Int) = src.get(col, row) & 1
  def getDouble(col: Int, row: Int) = i2d(src.get(col, row) & 1)
}

// Bytes
case class ByteRawConvert(src: Op) extends ConvertOp {
  def cellType = ByteCellType
  def get(col: Int, row: Int) = src.get(col, row).toByte
  def getDouble(col: Int, row: Int) = src.get(col, row).toByte.toDouble
}
case class ByteConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = ByteConstantNoDataCellType
  def get(col: Int, row: Int) = b2i(i2b(src.get(col, row)))
  def getDouble(col: Int, row: Int) = b2d(i2b(src.get(col, row)))
}
case class ByteUserDefinedNoDataConvert(src: Op, val userDefinedByteNoDataValue: Byte)
  extends ConvertOp with UserDefinedByteNoDataConversions {
  def cellType = ByteUserDefinedNoDataCellType(userDefinedByteNoDataValue)
  def get(col: Int, row: Int) = udb2i(i2udb(src.get(col, row)))
  def getDouble(col: Int, row: Int) = udb2d(i2udb(src.get(col, row)))
}


// UBytes
case class UByteRawConvert(src: Op) extends ConvertOp {
  def cellType = UByteCellType
  def get(col: Int, row: Int) = src.get(col, row).toByte & 0xFF
  def getDouble(col: Int, row: Int) = (src.get(col, row).toByte & 0xFF).toDouble
}
case class UByteConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = UByteConstantNoDataCellType
  def get(col: Int, row: Int) = ub2i(i2ub(src.get(col, row)))
  def getDouble(col: Int, row: Int) = ub2d(i2ub(src.get(col, row)))
}
case class UByteUserDefinedNoDataConvert(src: Op, val cellType: UByteUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue: Byte = cellType.noDataValue
  def get(col: Int, row: Int) = udub2i(i2udb(src.get(col, row)))
  def getDouble(col: Int, row: Int) = udub2d(i2udb(src.get(col, row)))
}

// Short
case class ShortRawConvert(src: Op) extends ConvertOp {
  def cellType = ShortCellType
  def get(col: Int, row: Int) = src.get(col, row).toShort
  def getDouble(col: Int, row: Int) = src.get(col, row).toShort.toDouble
}
case class ShortConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = ShortConstantNoDataCellType
  def get(col: Int, row: Int) = s2i(i2s(src.get(col, row)))
  def getDouble(col: Int, row: Int) = s2d(i2s(src.get(col, row)))
}
case class ShortUserDefinedNoDataConvert(src: Op, val cellType: ShortUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue: Short = cellType.noDataValue
  def get(col: Int, row: Int) = uds2i(i2uds(src.get(col, row)))
  def getDouble(col: Int, row: Int) = uds2d(i2uds(src.get(col, row)))
}

// UShort
case class UShortRawConvert(src: Op) extends ConvertOp {
  def cellType = UShortCellType
  def get(col: Int, row: Int) = src.get(col, row).toShort & 0xFFFF
  def getDouble(col: Int, row: Int) = (src.get(col, row).toShort & 0xFFFF).toDouble
}
case class UShortConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = UShortConstantNoDataCellType
  def get(col: Int, row: Int) = us2i(i2us(src.get(col, row)))
  def getDouble(col: Int, row: Int) = us2d(i2us(src.get(col, row)))
}
case class UShortUserDefinedNoDataConvert(src: Op, val cellType: UShortUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue: Short = cellType.noDataValue
  def get(col: Int, row: Int) = udus2i(i2uds(src.get(col, row)))
  def getDouble(col: Int, row: Int) = udus2d(i2uds(src.get(col, row)))
}

// Int
case class IntRawConvert(src: Op) extends ConvertOp {
  def cellType = IntCellType
  def get(col: Int, row: Int) = src.get(col, row)
  def getDouble(col: Int, row: Int) = src.get(col, row).toDouble
}
case class IntConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = IntConstantNoDataCellType
  def get(col: Int, row: Int) = src.get(col, row)
  def getDouble(col: Int, row: Int) = i2d(src.get(col, row))
}
case class IntUserDefinedNoDataConvert(src: Op, val cellType: IntUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedIntNoDataConversions {
  val userDefinedIntNoDataValue: Int = cellType.noDataValue
  def get(col: Int, row: Int) = udi2i(i2udi(src.get(col, row)))
  def getDouble(col: Int, row: Int) = udi2d(i2udi(src.get(col, row)))
}

// Float
case class FloatRawConvert(src: Op) extends ConvertOp {
  def cellType = FloatCellType
  def get(col: Int, row: Int) = src.getDouble(col, row).toFloat.toInt
  def getDouble(col: Int, row: Int) = src.getDouble(col, row).toFloat.toDouble
}
case class FloatConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = FloatConstantNoDataCellType
  def get(col: Int, row: Int) = f2i(d2f(src.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = f2d(d2f(src.getDouble(col, row)))
}
case class FloatUserDefinedNoDataConvert(src: Op, val cellType: FloatUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedFloatNoDataConversions {
  val userDefinedFloatNoDataValue: Float = cellType.noDataValue
  def get(col: Int, row: Int) = udf2i(d2udf(src.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = udf2d(d2udf(src.getDouble(col, row)))
}

// Double
case class DoubleRawConvert(src: Op) extends ConvertOp {
  def cellType = DoubleCellType
  def get(col: Int, row: Int) = src.getDouble(col, row).toInt
  def getDouble(col: Int, row: Int) = src.getDouble(col, row)
}
case class DoubleConstantNoDataConvert(src: Op) extends ConvertOp {
  def cellType = DoubleConstantNoDataCellType
  def get(col: Int, row: Int) = d2i(src.getDouble(col, row))
  def getDouble(col: Int, row: Int) = src.getDouble(col, row)
}
case class DoubleUserDefinedNoDataConvert(src: Op, val cellType: DoubleUserDefinedNoDataCellType)
  extends ConvertOp with UserDefinedDoubleNoDataConversions {
  val userDefinedDoubleNoDataValue: Double = cellType.noDataValue
  def get(col: Int, row: Int) = udd2i(d2udd(src.getDouble(col, row)))
  def getDouble(col: Int, row: Int) = udd2d(d2udd(src.getDouble(col, row)))
}
