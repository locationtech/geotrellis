package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags.codes.SampleFormat._

sealed abstract trait BandType {
  def bytesPerSample: Int = (bitsPerSample + 7) / 8
  def bitsPerSample: Int

  // If it's unsigned, we have to handle the conversion to JVM types carefully.
  def unsigned: Boolean

  /** SampleFormat code as defined by the TIFF spec */
  def sampleFormat: Int
}

object BandType {
  def apply(bitsPerSample: Int, sampleFormat: Int): BandType =
    (bitsPerSample, sampleFormat) match {
      case (1, _) => BitBandType
      case (8, UnsignedInt) => UByteBandType
      case (8, SignedInt) => ByteBandType
      case (16, UnsignedInt) => UInt16BandType
      case (16, SignedInt) => Int16BandType
      case (32, UnsignedInt) => UInt32BandType
      case (32, SignedInt) => Int32BandType
      case (32, FloatingPoint) => Float32BandType
      case (64, FloatingPoint) => Float64BandType
      case _ =>
        throw new UnsupportedOperationException(s"Unsupported band type ($bitsPerSample, $sampleFormat)")
    }

  def forCellType(cellType: CellType): BandType =
    cellType match {
      case BitCellType => BitBandType
      case ByteConstantNoDataCellType | ByteCellType | ByteUserDefinedNoDataCellType(_) => ByteBandType
      case UByteConstantNoDataCellType | UByteCellType | UByteUserDefinedNoDataCellType(_) => UByteBandType
      case ShortConstantNoDataCellType | ShortCellType | ShortUserDefinedNoDataCellType(_) => Int16BandType
      case UShortConstantNoDataCellType | UShortCellType | UShortUserDefinedNoDataCellType(_) => UInt16BandType
      case IntConstantNoDataCellType | IntCellType | IntUserDefinedNoDataCellType(_) => Int32BandType
      case FloatConstantNoDataCellType | FloatCellType | FloatUserDefinedNoDataCellType(_) => Float32BandType
      case DoubleConstantNoDataCellType | DoubleCellType | DoubleUserDefinedNoDataCellType(_) => Float64BandType
    }
}

object BitBandType extends BandType {
  val bitsPerSample = 1
  val unsigned = false
  val sampleFormat = SignedInt
}

object UByteBandType extends BandType {
  val bitsPerSample = 8
  val unsigned = true
  val sampleFormat = UnsignedInt
}

object ByteBandType extends BandType {
  val bitsPerSample = 8
  val unsigned = false
  val sampleFormat = SignedInt
}

object UInt16BandType extends BandType {
  val bitsPerSample = 16
  val unsigned = true
  val sampleFormat = UnsignedInt
}

object Int16BandType extends BandType {
  val bitsPerSample = 16
  val unsigned = false
  val sampleFormat = SignedInt
}

object UInt32BandType extends BandType {
  val bitsPerSample = 32
  val unsigned = true
  val sampleFormat = UnsignedInt
}

object Int32BandType extends BandType {
  val bitsPerSample = 32
  val unsigned = false
  val sampleFormat = SignedInt
}

object Float32BandType extends BandType {
  val bitsPerSample = 32
  val unsigned = false
  val sampleFormat = FloatingPoint
}

object Float64BandType extends BandType {
  val bitsPerSample = 64
  val unsigned = false
  val sampleFormat = FloatingPoint
}

// Complex types are not supported
// object CInt16BandType extends BandType {
//   val bitsPerSample = 16
//   val unsigned = false
//   val cellType: CellType = ???
// }

// object CInt32BandType extends BandType {
//   val bitsPerSample = 32
//   val unsigned = false
//   val cellType: CellType = ???
// }

// object CFloat32 extends BandType {
//   val bitsPerSample = 32
//   val unsigned = false
//   val cellType: CellType = ???
// }

// object CFloat64 extends BandType {
//   val bitsPerSample = 64
//   val unsigned = false
//   val cellType: CellType = ???
// }
