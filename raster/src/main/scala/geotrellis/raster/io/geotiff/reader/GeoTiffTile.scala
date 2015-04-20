package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.interpolation.InterpolationMethod
import geotrellis.vector.Extent

import spire.syntax.cfor._

// Left to implement for Tile:
//   convert?
//    def combine(r2: geotrellis.raster.Tile)(f: (Int, Int) => Int): geotrellis.raster.Tile = ???
//    def combineDouble(r2: geotrellis.raster.Tile)(f: (Double, Double) => Double): geotrellis.raster.Tile = ???
//    def resample(source: geotrellis.vector.Extent,target: geotrellis.raster.RasterExtent,method: geotrellis.raster.interpolation.InterpolationMethod): geotrellis.raster.Tile = ???

object GeoTiffTile {
  def apply(
    bandType: BandType,   
    compressedBytes: Array[Array[Byte]],
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    compression: Compression,
    noDataValue: Option[Double]
  ): GeoTiffTile =
    bandType match {
      case BitBandType     => new BitGeoTiffTile(compressedBytes, decompressor, segmentLayout, compression)
      case ByteBandType    => new ByteGeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case UInt16BandType  => new UInt16GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case Int16BandType   => new Int16GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case UInt32BandType  => new UInt32GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case Int32BandType   => new Int32GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case Float32BandType => new Float32GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
      case Float64BandType => new Float64GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    }
}

abstract class GeoTiffTile(
  compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  val segmentLayout: GeoTiffSegmentLayout,
  compression: Compression // Compression to use moving forward
) extends Tile {
  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  def convert(cellType: CellType): Tile = ??? // TODO: Remove?

  val segmentCount = compressedBytes.size

  def getDecompressedBytes(i: Int): Array[Byte] =
    decompressor.decompress(compressedBytes(i), i)

  def getSegment(i: Int): GeoTiffSegment

  def get(col: Int, row: Int): Int = {
    val segmentIndex = segmentLayout.getSegmentIndex(col, row)
    val i = segmentLayout.getSegmentTransform(segmentIndex).gridToIndex(col, row)
    getSegment(segmentIndex).getInt(i)
  }

  def getDouble(col: Int, row: Int): Double = {
    val segmentIndex = segmentLayout.getSegmentIndex(col, row)
    val i = segmentLayout.getSegmentTransform(segmentIndex).gridToIndex(col, row)
    getSegment(segmentIndex).getDouble(i)
  }

  def foreach(f: Int => Unit): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size
      cfor(0)(_ < segmentSize, _ + 1) { i =>
        f(segment.getInt(i))
      }
    }
  }

  def foreachDouble(f: Double => Unit): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size
      cfor(0)(_ < segmentSize, _ + 1) { i =>
        f(segment.getDouble(i))
      }
    }
  }

  def map(f: Int => Int): GeoTiffTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.map(f)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      Int32BandType,
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def mapDouble(f: Double => Double): GeoTiffTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.mapDouble(f)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      Float64BandType,
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segmentSize, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        visitor(col, row, segment.getInt(i))
      }
    }
  }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segmentSize, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        visitor(col, row, segment.getDouble(i))
      }
    }
  }
   
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      val newBytes = segment.mapWithIndex { (i, z) =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        mapper(col, row, z)
      }
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      Int32BandType,
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      val newBytes = segment.mapDoubleWithIndex { (i, z) =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        mapper(col, row, z)
      }
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      Float64BandType,
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def combine(other: Tile)(f: (Int, Int) => Int): Tile =
    other match {
      case otherGeoTiff: GeoTiffTile if segmentLayout.tileLayout == otherGeoTiff.segmentLayout.tileLayout =>
        // GeoTiffs with the same segment sizes, can map over segments.
        val arr = Array.ofDim[Array[Byte]](segmentCount)
        val compressor = compression.createCompressor(segmentCount)
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val otherSegment = otherGeoTiff.getSegment(segmentIndex)
          val newBytes = segment.mapWithIndex { (i, z) =>
            f(z, otherSegment.getInt(i))
          }
          arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
        }

        GeoTiffTile(
          Int32BandType,
          arr,
          compressor.createDecompressor(),
          segmentLayout,
          compression,
          None
        )
      case _ =>
        this.map { (col, row, z) =>
          f(z, other.get(col, row))
        }
    }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = 
    other match {
      case otherGeoTiff: GeoTiffTile if segmentLayout.tileLayout == otherGeoTiff.segmentLayout.tileLayout =>
        // GeoTiffs with the same segment sizes, can map over segments.
        val arr = Array.ofDim[Array[Byte]](segmentCount)
        val compressor = compression.createCompressor(segmentCount)
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val otherSegment = otherGeoTiff.getSegment(segmentIndex)
          val newBytes = segment.mapDoubleWithIndex { (i, z) =>
            f(z, otherSegment.getDouble(i))
          }
          arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
        }

        GeoTiffTile(
          Float64BandType,
          arr,
          compressor.createDecompressor(),
          segmentLayout,
          compression,
          None
        )
      case _ =>
        this.mapDouble { (col, row, z) =>
          f(z, other.get(col, row))
        }
    }


  def resample(source: Extent, target: RasterExtent, method: InterpolationMethod): Tile = ???

  def toArray(): Array[Int] = 
    toArrayTile.toArray

  def toArrayDouble(): Array[Double] =
    toArrayTile.toArrayDouble

  def toArrayTile(): ArrayTile = mutable

  def mutable: MutableArrayTile

  def toBytes(): Array[Byte] =
    toArrayTile.toBytes
}

class BitGeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: BitGeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private def createSegment(i: Int): BitGeoTiffSegment = 
    new BitGeoTiffSegment(getDecompressedBytes(i), segmentLayout.getSegmentSize(i))

  val cellType = TypeBit

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val result = BitArrayTile.empty(cols, rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = 
        if(segmentIndex == _lastSegmentIndex) _lastSegment
        else createSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          result.set(col, row, segment.get(i))
        }
      }
    }
   result
  }
}

class ByteGeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: ByteGeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => ByteGeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Byte.MinValue.toDouble <= nd && nd <= Byte.MaxValue.toDouble =>
        { i: Int => new NoDataByteGeoTiffSegment(getDecompressedBytes(i), nd.toByte) }
      case _ =>
        { i: Int => new ByteGeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeByte

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      val arr = Array.ofDim[Byte](cols * rows)
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols
        val tileWidth = segmentLayout.tileLayout.tileCols

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          val j = (row * cols) + col
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Byte.MinValue.toDouble <= nd && nd <= Byte.MaxValue.toDouble =>
        ByteArrayTile.fromBytes(arr, cols, rows, nd.toByte)
      case _ =>
        ByteArrayTile.fromBytes(arr, cols, rows)
    }
  }
}

class UInt16GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: UInt16GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => UInt16GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataUInt16GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new UInt16GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeInt

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Int](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = 
        if(segmentIndex == _lastSegmentIndex) _lastSegment
        else createSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.get(i)
        }
      }
    }
    IntArrayTile(arr, cols, rows)
  }
}

class Int16GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: Int16GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Int16GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        { i: Int => new NoDataInt16GeoTiffSegment(getDecompressedBytes(i), nd.toShort) }
      case _ =>
        { i: Int => new Int16GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeShort

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeShort.bytes)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * TypeShort.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeShort.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeShort.bytes)
          val row = segmentTransform.indexToRow(i / TypeShort.bytes)
          val j = ((row * cols) + col) * TypeShort.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Short.MinValue.toDouble <= nd && nd <= Short.MaxValue.toDouble =>
        ShortArrayTile.fromBytes(arr, cols, rows, nd.toShort)
      case _ =>
        ShortArrayTile.fromBytes(arr, cols, rows)
    }
  }
}

class UInt32GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: UInt32GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => UInt32GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataUInt32GeoTiffSegment(getDecompressedBytes(i), nd.toFloat) }
      case _ =>
        { i: Int => new UInt32GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeFloat

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Float](cols * rows)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = 
        if(segmentIndex == _lastSegmentIndex) _lastSegment
        else createSegment(segmentIndex)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
      cfor(0)(_ < segment.size, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          arr(row * cols + col) = segment.get(i)
        }
      }
    }
    FloatArrayTile(arr, cols, rows)
  }
}

class Int32GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: Int32GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Int32GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        { i: Int => new NoDataInt32GeoTiffSegment(getDecompressedBytes(i), nd.toInt) }
      case _ =>
        { i: Int => new Int32GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeInt

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeInt.bytes)
    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * TypeInt.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeInt.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeInt.bytes)
          val row = segmentTransform.indexToRow(i / TypeInt.bytes)
          val j = ((row * cols) + col) * TypeInt.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }

    noDataValue match {
      case Some(nd) if isData(nd) && Int.MinValue.toDouble <= nd && nd <= Int.MaxValue.toDouble =>
        IntArrayTile.fromBytes(arr, cols, rows, nd.toInt)
      case _ =>
        IntArrayTile.fromBytes(arr, cols, rows)
    }
  }
}

class Float32GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: Float32GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Float32GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataFloat32GeoTiffSegment(getDecompressedBytes(i), nd.toFloat) }
      case _ =>
        { i: Int => new Float32GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeFloat

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeFloat.bytes)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * TypeFloat.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeFloat.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeFloat.bytes)
          val row = segmentTransform.indexToRow(i / TypeFloat.bytes)
          val j = ((row * cols) + col) * TypeFloat.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    noDataValue match {
      case Some(nd) if isData(nd) && Float.MinValue.toDouble <= nd && nd <= Float.MaxValue.toDouble =>
        FloatArrayTile.fromBytes(arr, cols, rows, nd.toFloat)
      case _ =>
        FloatArrayTile.fromBytes(arr, cols, rows)
    }
  }
}

class Float64GeoTiffTile(compressedBytes: Array[Array[Byte]],
  decompressor: Decompressor,
  segmentLayout: GeoTiffSegmentLayout,
  compression: Compression,
  noDataValue: Option[Double]
) extends GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression) {
  // Cached last segment
  private var _lastSegment: Float64GeoTiffSegment = null
  private var _lastSegmentIndex: Int = -1

  private val createSegment: Int => Float64GeoTiffSegment = 
    noDataValue match {
      case Some(nd) if isData(nd) =>
        { i: Int => new NoDataFloat64GeoTiffSegment(getDecompressedBytes(i), nd) }
      case _ =>
        { i: Int => new Float64GeoTiffSegment(getDecompressedBytes(i)) }
    }

  val cellType = TypeDouble

  def getSegment(i: Int): GeoTiffSegment = {
    if(i != _lastSegmentIndex) {
      _lastSegment = createSegment(i)
      _lastSegmentIndex = i 
    }
    _lastSegment
  }

  def mutable: MutableArrayTile = {
    val arr = Array.ofDim[Byte](cols * rows * TypeDouble.bytes)

    if(segmentLayout.isStriped) {
      var i = 0
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)
        val size = segment.bytes.size
        System.arraycopy(segment.bytes, 0, arr, i, size)
        i += size
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment =
          if(segmentIndex == _lastSegmentIndex) _lastSegment
          else createSegment(segmentIndex)

        val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
        val width = segmentTransform.segmentCols * TypeDouble.bytes
        val tileWidth = segmentLayout.tileLayout.tileCols * TypeDouble.bytes

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / TypeDouble.bytes)
          val row = segmentTransform.indexToRow(i / TypeDouble.bytes)
          val j = ((row * cols) + col) * TypeDouble.bytes
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    }
    noDataValue match {
      case Some(nd) if isData(nd) =>
        DoubleArrayTile.fromBytes(arr, cols, rows, nd)
      case _ =>
        DoubleArrayTile.fromBytes(arr, cols, rows)
    }
  }
}
