package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.interpolation.InterpolationMethod
import geotrellis.raster.io.geotiff.compression._
import geotrellis.vector.Extent

import java.util.BitSet

import spire.syntax.cfor._

object GeoTiffMultiBandTile {
  def apply(
    bandType: BandType,
    compressedBytes: Array[Array[Byte]],
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    bandCount: Int,
    hasPixelInterleave: Boolean,
    compression: Compression
  ): GeoTiffMultiBandTile =
    apply(bandType, compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, None)

  def apply(
    bandType: BandType,
    compressedBytes: Array[Array[Byte]],
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    compression: Compression,
    bandCount: Int,
    hasPixelInterleave: Boolean,
    noDataValue: Option[Double]
  ): GeoTiffMultiBandTile =
    new GeoTiffMultiBandTile(bandType, compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
    // bandType match {
    //   // case BitBandType     => new BitGeoTiffTile(compressedBytes, decompressor, segmentLayout, compression)
    //   // case ByteBandType    => new ByteGeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   // case UInt16BandType  => new UInt16GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   // case Int16BandType   => new Int16GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   // case UInt32BandType  => new UInt32GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   case Int32BandType   => new Int32GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   // case Float32BandType => new Float32GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   // case Float64BandType => new Float64GeoTiffTile(compressedBytes, decompressor, segmentLayout, compression, noDataValue)
    //   case _ => ???
    // }

  // /** Convert a multiband tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. Only handles pixel interlacing. */
  def apply(tile: MultiBandTile): GeoTiffMultiBandTile =
    apply(tile, GeoTiffOptions.DEFAULT)

  def apply(tile: MultiBandTile, options: GeoTiffOptions): GeoTiffMultiBandTile = {
    val bandType = BandType.forCellType(tile.cellType)
    val bandCount = tile.bandCount

    val segmentLayout = GeoTiffSegmentLayout(tile.cols, tile.rows, options.storageMethod, bandType)

    val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
    val compressor = options.compression.createCompressor(segmentCount)

    val compressedBytes = Array.ofDim[Array[Byte]](segmentCount)

    val segmentTiles = Array.ofDim[Array[Tile]](segmentCount)
    cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
      val bandTiles =
        options.storageMethod match {
          case _: Tiled => CompositeTile.split(tile.band(bandIndex), segmentLayout.tileLayout)
          case _: Striped => CompositeTile.split(tile.band(bandIndex), segmentLayout.tileLayout, extend = false)
        }

      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val bandTile = bandTiles(segmentIndex)
        if(bandIndex == 0) { segmentTiles(segmentIndex) = Array.ofDim[Tile](bandCount) }
        segmentTiles(segmentIndex)(bandIndex) = bandTile
      }
    }

    val byteCount = tile.cellType.bytes

    cfor(0)(_ < segmentCount, _ + 1) { i =>
      val tiles = segmentTiles(i)
      val cols = tiles(0).cols
      val rows = tiles(0).rows
      var segmentByteCount = 0
      val segmentBytes = Array.ofDim[Byte](cols * rows * bandCount * byteCount)

      val tileBytes = Array.ofDim[Array[Byte]](bandCount)
      cfor(0)(_ < bandCount, _ + 1) { b =>
        tileBytes(b) = tiles(b).toBytes
      }

      var segmentIndex = 0
      cfor(0)(_ < cols * rows, _ + 1) { cellIndex =>
        cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
          cfor(0)(_ < byteCount, _ + 1) { b =>
            val bytes = tileBytes(bandIndex)
            segmentBytes(segmentIndex) = bytes(cellIndex * byteCount + b)
            segmentIndex += 1
          }
        }
      }

      compressedBytes(i) = compressor.compress(segmentBytes, i)
    }

    apply(bandType, compressedBytes, compressor.createDecompressor, segmentLayout, bandCount, true, options.compression)
  }

}

class GeoTiffMultiBandTile(
  val bandType: BandType,
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  val segmentLayout: GeoTiffSegmentLayout,
  val compression: Compression,
  val bandCount: Int,
  hasPixelInterleave: Boolean,
  noDataValue: Option[Double]
) extends MultiBandTile with GeoTiffImageData {
  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  def cellType: CellType = bandType.cellType

  def getDecompressedBytes(i: Int): Array[Byte] =
    decompressor.decompress(compressedBytes(i), i)


  def band(bandIndex: Int): GeoTiffTile = {
    if(bandIndex >= bandCount) { throw new IllegalArgumentException(s"Band $bandIndex does not exist") }
    if(hasPixelInterleave) {
      val size = compressedBytes.size
      val compressedBandBytes = Array.ofDim[Array[Byte]](size)
      val compressor = compression.createCompressor(size)
      val bytesPerSample = bandType.bytesPerSample
      val bytesPerCell = bytesPerSample * bandCount

      cfor(0)(_ < size, _ + 1) { segmentIndex =>
        val segment = getDecompressedBytes(segmentIndex)
        val segmentSize = segment.size
        val bandSegmentSize = segmentSize / bandCount
        val bandSegment = Array.ofDim[Byte](bandSegmentSize)

        var j = 0
        cfor(bandIndex * bytesPerSample)(_ < segmentSize, _ + bytesPerCell) { i =>
          var b = 0
          while(b < bytesPerSample) { bandSegment(j + b) = segment(i + b) ; b += 1 }
          j += bytesPerSample
        }

        compressedBandBytes(segmentIndex) = compressor.compress(bandSegment, segmentIndex)
      }

      GeoTiffTile(bandType, compressedBandBytes, compressor.createDecompressor(), segmentLayout, compression)
    } else {
      val size = compressedBytes.size
      val compressedBandBytes = Array.ofDim[Array[Byte]](size / bandCount)

      var j = 0
      cfor(bandIndex)(_ < size, _ + bandCount) { i =>
        compressedBandBytes(j) = compressedBytes(i).clone
        j += 1
      }

      GeoTiffTile(bandType, compressedBandBytes, decompressor, segmentLayout, compression)
    }
  }
}

//abstract class GeoTiffTile(
// class GeoTiffTile(
//   compressedBytes: Array[Array[Byte]],
//   decompressor: Decompressor,
//   val segmentLayout: GeoTiffSegmentLayout,
//   compression: Compression // Compression to use moving forward
// ) extends Tile {
//   val cols: Int = segmentLayout.totalCols
//   val rows: Int = segmentLayout.totalRows

//   def storageMethod: StorageMethod = 
//     segmentLayout.storageMethod
//   def geoTiffOptions: GeoTiffOptions =
//     GeoTiffOptions(storageMethod, compression)

//   // def band(i: Int): GeoTiffTile = 
//   //   if(has

//   // def convert(newCellType: CellType): Tile = {
//   //   val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //   val compressor = compression.createCompressor(segmentCount)
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val newBytes = segment.convert(cellType)
//   //     arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //   }

//   //   GeoTiffTile(
//   //     BandType.forCellType(newCellType),
//   //     arr,
//   //     compressor.createDecompressor(),
//   //     segmentLayout,
//   //     compression,
//   //     None
//   //   )
//   // }

//   // val segmentCount = compressedBytes.size

//   // def getDecompressedBytes(i: Int): Array[Byte] =
//   //   decompressor.decompress(compressedBytes(i), i)

//   // def getSegment(i: Int): GeoTiffSegment

//   // def get(col: Int, row: Int): Int = {
//   //   val segmentIndex = segmentLayout.getSegmentIndex(col, row)
//   //   val i = segmentLayout.getSegmentTransform(segmentIndex).gridToIndex(col, row)
//   //   getSegment(segmentIndex).getInt(i)
//   // }

//   // def getDouble(col: Int, row: Int): Double = {
//   //   val segmentIndex = segmentLayout.getSegmentIndex(col, row)
//   //   val i = segmentLayout.getSegmentTransform(segmentIndex).gridToIndex(col, row)
//   //   getSegment(segmentIndex).getDouble(i)
//   // }

//   // def foreach(f: Int => Unit): Unit = {
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentSize = segment.size
//   //     cfor(0)(_ < segmentSize, _ + 1) { i =>
//   //       f(segment.getInt(i))
//   //     }
//   //   }
//   // }

//   // def foreachDouble(f: Double => Unit): Unit = {
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentSize = segment.size
//   //     cfor(0)(_ < segmentSize, _ + 1) { i =>
//   //       f(segment.getDouble(i))
//   //     }
//   //   }
//   // }

//   // def map(f: Int => Int): GeoTiffTile = {
//   //   val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //   val compressor = compression.createCompressor(segmentCount)
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val newBytes = segment.map(f)
//   //     arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //   }

//   //   GeoTiffTile(
//   //     BandType.forCellType(cellType),
//   //     arr,
//   //     compressor.createDecompressor(),
//   //     segmentLayout,
//   //     compression,
//   //     None
//   //   )
//   // }

//   // def mapDouble(f: Double => Double): GeoTiffTile = {
//   //   val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //   val compressor = compression.createCompressor(segmentCount)
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val newBytes = segment.mapDouble(f)
//   //     arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //   }

//   //   GeoTiffTile(
//   //     BandType.forCellType(cellType),
//   //     arr,
//   //     compressor.createDecompressor(),
//   //     segmentLayout,
//   //     compression,
//   //     None
//   //   )
//   // }

//   // def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentSize = segment.size
//   //     val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
//   //     cfor(0)(_ < segmentSize, _ + 1) { i =>
//   //       val col = segmentTransform.indexToCol(i)
//   //       val row = segmentTransform.indexToRow(i)
//   //       if(col < cols && row < rows) {
//   //         visitor(col, row, segment.getInt(i))
//   //       }
//   //     }
//   //   }
//   // }

//   // def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentSize = segment.size
//   //     val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
//   //     cfor(0)(_ < segmentSize, _ + 1) { i =>
//   //       val col = segmentTransform.indexToCol(i)
//   //       val row = segmentTransform.indexToRow(i)
//   //       if(col < cols && row < rows) {
//   //         visitor(col, row, segment.getDouble(i))
//   //       }
//   //     }
//   //   }
//   // }
   
//   // def mapIntMapper(mapper: IntTileMapper): Tile = {
//   //   val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //   val compressor = compression.createCompressor(segmentCount)
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
//   //     val newBytes = segment.mapWithIndex { (i, z) =>
//   //       val col = segmentTransform.indexToCol(i)
//   //       val row = segmentTransform.indexToRow(i)
//   //       if(col < cols && row < rows) {
//   //         mapper(col, row, z)
//   //       } else { 0 }

//   //     }
//   //     arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //   }

//   //   GeoTiffTile(
//   //     BandType.forCellType(cellType),
//   //     arr,
//   //     compressor.createDecompressor(),
//   //     segmentLayout,
//   //     compression,
//   //     None
//   //   )
//   // }

//   // def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
//   //   val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //   val compressor = compression.createCompressor(segmentCount)
//   //   cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //     val segment = getSegment(segmentIndex)
//   //     val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
//   //     val newBytes = segment.mapDoubleWithIndex { (i, z) =>
//   //       val col = segmentTransform.indexToCol(i)
//   //       val row = segmentTransform.indexToRow(i)
//   //       if(col < cols && row < rows) {
//   //         mapper(col, row, z)
//   //       } else { 0.0 }
//   //     }
//   //     arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //   }

//   //   GeoTiffTile(
//   //     BandType.forCellType(cellType),
//   //     arr,
//   //     compressor.createDecompressor(),
//   //     segmentLayout,
//   //     compression,
//   //     None
//   //   )
//   // }

//   // def combine(other: Tile)(f: (Int, Int) => Int): Tile =
//   //   other match {
//   //     case otherGeoTiff: GeoTiffTile if segmentLayout.tileLayout == otherGeoTiff.segmentLayout.tileLayout =>
//   //       // GeoTiffs with the same segment sizes, can map over segments.
//   //       val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //       val compressor = compression.createCompressor(segmentCount)
//   //       cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //         val segment = getSegment(segmentIndex)
//   //         val otherSegment = otherGeoTiff.getSegment(segmentIndex)
//   //         val newBytes = segment.mapWithIndex { (i, z) =>
//   //           f(z, otherSegment.getInt(i))
//   //         }
//   //         arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //       }

//   //       GeoTiffTile(
//   //         BandType.forCellType(cellType.union(other.cellType)),
//   //         arr,
//   //         compressor.createDecompressor(),
//   //         segmentLayout,
//   //         compression,
//   //         None
//   //       )
//   //     case _ =>
//   //       this.map { (col, row, z) =>
//   //         f(z, other.get(col, row))
//   //       }
//   //   }

//   // def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = 
//   //   other match {
//   //     case otherGeoTiff: GeoTiffTile if segmentLayout.tileLayout == otherGeoTiff.segmentLayout.tileLayout =>
//   //       // GeoTiffs with the same segment sizes, can map over segments.
//   //       val arr = Array.ofDim[Array[Byte]](segmentCount)
//   //       val compressor = compression.createCompressor(segmentCount)
//   //       cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
//   //         val segment = getSegment(segmentIndex)
//   //         val otherSegment = otherGeoTiff.getSegment(segmentIndex)
//   //         val newBytes = segment.mapDoubleWithIndex { (i, z) =>
//   //           f(z, otherSegment.getDouble(i))
//   //         }
//   //         arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
//   //       }

//   //       GeoTiffTile(
//   //         BandType.forCellType(cellType),
//   //         arr,
//   //         compressor.createDecompressor(),
//   //         segmentLayout,
//   //         compression,
//   //         None
//   //       )
//   //     case _ =>
//   //       this.mapDouble { (col, row, z) =>
//   //         f(z, other.get(col, row))
//   //       }
//   //   }


//   // def resample(source: Extent, target: RasterExtent, method: InterpolationMethod): Tile =
//   //   Resample(this, source, target, method)

//   // def toArray(): Array[Int] = 
//   //   toArrayTile.toArray

//   // def toArrayDouble(): Array[Double] =
//   //   toArrayTile.toArrayDouble

//   // def toArrayTile(): ArrayTile = mutable

//   // def mutable: MutableArrayTile

//   // def toBytes(): Array[Byte] =
//   //   toArrayTile.toBytes
//     }
