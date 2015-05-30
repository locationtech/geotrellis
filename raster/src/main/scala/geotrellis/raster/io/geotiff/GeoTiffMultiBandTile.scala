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
    bandType match {
      case BitBandType     => 
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with BitGeoTiffSegmentCollection
      case ByteBandType    => 
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with ByteGeoTiffSegmentCollection
      case UInt16BandType  => 
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with UInt16GeoTiffSegmentCollection
      case Int16BandType   =>
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with Int16GeoTiffSegmentCollection
      case UInt32BandType  =>
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with UInt32GeoTiffSegmentCollection
      case Int32BandType   =>
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with Int32GeoTiffSegmentCollection
      case Float32BandType =>
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with Float32GeoTiffSegmentCollection
      case Float64BandType =>
        new GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
            with Float64GeoTiffSegmentCollection
    }

  /** Convert a multiband tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. Only handles pixel interlacing. */
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

abstract class GeoTiffMultiBandTile(
  val compressedBytes: Array[Array[Byte]],
  val decompressor: Decompressor,
  val segmentLayout: GeoTiffSegmentLayout,
  val compression: Compression,
  val bandCount: Int,
  hasPixelInterleave: Boolean,
  val noDataValue: Option[Double]
) extends MultiBandTile with GeoTiffImageData {
  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  def getSegment(i: Int): GeoTiffSegment

  val segmentCount = compressedBytes.size
  private val isTiled = segmentLayout.isTiled

  def band(bandIndex: Int): GeoTiffTile = {
    if(bandIndex >= bandCount) { throw new IllegalArgumentException(s"Band $bandIndex does not exist") }
    if(hasPixelInterleave) {
      val size = compressedBytes.size
      val compressedBandBytes = Array.ofDim[Array[Byte]](size)
      val compressor = compression.createCompressor(size)
      val bytesPerSample = bandType.bytesPerSample
      val bytesPerCell = bytesPerSample * bandCount

      cfor(0)(_ < size, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex).bytes
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

  def convert(newCellType: CellType): MultiBandTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.convert(newCellType)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffMultiBandTile(
      BandType.forCellType(newCellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      None
    )
  }


  def map(b0: Int)(f: Int => Int): MultiBandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    if(hasPixelInterleave) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        val newBytes = segment.mapWithIndex { (i, z) =>
          if(i % bandCount == b0)
            f(z)
          else
            z
        }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        if(segmentIndex % bandCount == b0) {
          val newBytes = segment.map(f)
          arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
        } else {
          arr(segmentIndex) = compressor.compress(segment.bytes, segmentIndex)
        }
      }
    }

    GeoTiffMultiBandTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      None
    )
  }

  def map(f: (Int, Int) => Int): MultiBandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    if(hasPixelInterleave) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        val newBytes = segment.mapWithIndex { (i, z) =>
          f(i % bandCount, z)
        }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val bandIndex = segmentIndex % bandCount
        val segment = getSegment(segmentIndex)
        val newBytes = segment.map { z => f(bandIndex, z) }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    }

    GeoTiffMultiBandTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      None
    )
  }

  def mapDouble(b0: Int)(f: Double => Double): MultiBandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    if(hasPixelInterleave) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        val newBytes = segment.mapDoubleWithIndex { (i, z) =>
          if(i % bandCount == b0)
            f(z)
          else
            z
        }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        if(segmentIndex % bandCount == b0) {
          val newBytes = segment.mapDouble(f)
          arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
        } else {
          arr(segmentIndex) = compressor.compress(segment.bytes, segmentIndex)
        }
      }
    }

    GeoTiffMultiBandTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      None
    )
  }

  def mapDouble(f: (Int, Double) => Double): MultiBandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    if(hasPixelInterleave) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        val newBytes = segment.mapDoubleWithIndex { (i, z) =>
          f(i % bandCount, z)
        }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    } else {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val bandIndex = segmentIndex % bandCount
        val segment = getSegment(segmentIndex)
        val newBytes = segment.mapDouble { z => f(bandIndex, z) }

        arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
      }
    }

    GeoTiffMultiBandTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      None
    )
  }

  def foreach(b0: Int)(f: Int => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            if(i % bandCount == b0) {
              val col = segmentTransform.indexToCol(i / bandCount)
              val row = segmentTransform.indexToRow(i / bandCount)
              if(col < cols && row < rows) {
                f(segment.getInt(i))
              }
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            if(i % bandCount == b0) {
              f(segment.getInt(i))
            }
          }
        }
      }
    } else {
      if(isTiled) {
        cfor(b0)(_ < segmentCount, _ + bandCount) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex / bandCount)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(segment.getInt(i))
            }
          }
        }
      } else {
        cfor(b0)(_ < segmentCount, _ + bandCount) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(segment.getInt(i))
          }
        }
      }
    }
  }

  def foreach(f: (Int, Int) => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i / bandCount)
            val row = segmentTransform.indexToRow(i / bandCount)
            if(col < cols && row < rows) {
              f(i % bandCount, segment.getInt(i))
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(i % bandCount, segment.getInt(i))
          }
        }
      }
    } else {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex / bandCount)
          val bandIndex = segmentIndex % bandCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(bandIndex, segment.getInt(i))
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val bandIndex = segmentIndex % bandCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(bandIndex, segment.getInt(i))
          }
        }
      }
    }
  }

  def foreachDouble(b0: Int)(f: Double => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            if(i % bandCount == b0) {
              val col = segmentTransform.indexToCol(i / bandCount)
              val row = segmentTransform.indexToRow(i / bandCount)
              if(col < cols && row < rows) {
                f(segment.getDouble(i))
              }
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            if(i % bandCount == b0) {
              f(segment.getDouble(i))
            }
          }
        }
      }
    } else {
      if(isTiled) {
        cfor(b0)(_ < segmentCount, _ + bandCount) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex / bandCount)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(segment.getDouble(i))
            }
          }
        }
      } else {
        cfor(b0)(_ < segmentCount, _ + bandCount) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(segment.getDouble(i))
          }
        }
      }
    }
  }

  def foreachDouble(f: (Int, Double) => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i / bandCount)
            val row = segmentTransform.indexToRow(i / bandCount)
            if(col < cols && row < rows) {
              f(i % bandCount, segment.getDouble(i))
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(i % bandCount, segment.getDouble(i))
          }
        }
      }
    } else {
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex / bandCount)
          val bandIndex = segmentIndex % bandCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(bandIndex, segment.getDouble(i))
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val bandIndex = segmentIndex % bandCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(bandIndex, segment.getDouble(i))
          }
        }
      }
    }
  }

  def combine(b0: Int,b1: Int)(f: (Int, Int) => Int): Tile = ???
  def combine(f: Array[Int] => Int): Tile = ???
  def combineDouble(b0: Int,b1: Int)(f: (Double, Double) => Double): Tile = ???
  def combineDouble(f: Array[Double] => Double): Tile = ???
  def combineDoubleTileCombiner(combiner: geotrellis.macros.DoubleTileCombiner3): geotrellis.raster.Tile = ???
  def combineDoubleTileCombiner(combiner: geotrellis.macros.DoubleTileCombiner4): geotrellis.raster.Tile = ???
  def combineIntTileCombiner(combiner: geotrellis.macros.IntTileCombiner3): geotrellis.raster.Tile = ???
  def combineIntTileCombiner(combiner: geotrellis.macros.IntTileCombiner4): geotrellis.raster.Tile = ???
}
