package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.resample.ResampleMethod
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
      case BitBandType => 
        new BitGeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case ByteBandType    => 
        new ByteGeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case UInt16BandType  => 
        new UInt16GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case Int16BandType   =>
        new Int16GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case UInt32BandType  =>
        new UInt32GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case Int32BandType   =>
        new Int32GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case Float32BandType =>
        new Float32GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
      case Float64BandType =>
        new Float64GeoTiffMultiBandTile(compressedBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, noDataValue)
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
  val hasPixelInterleave: Boolean,
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
      bandType match {
        case BitBandType =>
          val compressedBandBytes = Array.ofDim[Array[Byte]](segmentCount)
          val compressor = compression.createCompressor(segmentCount)
          val bytesPerSample = bandType.bytesPerSample
          val bytesPerCell = bytesPerSample * bandCount

          cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
            val segmentSize = segmentLayout.getSegmentSize(segmentIndex)
            val (cols, rows) =
              if(segmentLayout.isTiled) { (segmentLayout.tileLayout.tileCols, segmentLayout.tileLayout.tileRows) }
              else { segmentLayout.getSegmentDimensions(segmentIndex) }

            val paddedCols = {
              val bytesWidth = (cols + 7) / 8
              bytesWidth * 8
            }

            val resultByteCount =
              (paddedCols / 8) * rows

            val bandSegment = Array.ofDim[Byte](resultByteCount)
            val segment = getSegment(segmentIndex)

            cfor(bandIndex)(_ < segment.size, _ + bandCount) { i =>
              val j = i / bandCount

              val col = (j % cols)
              val row = (j / cols)
              val i2 = (row * paddedCols) + col

              BitArrayTile.update(bandSegment, i2, segment.getInt(i))
            }

            compressedBandBytes(segmentIndex) = compressor.compress(bandSegment, segmentIndex)
          }

          GeoTiffTile(bandType, compressedBandBytes, compressor.createDecompressor(), segmentLayout, compression)
        case _ =>
          val compressedBandBytes = Array.ofDim[Array[Byte]](segmentCount)
          val compressor = compression.createCompressor(segmentCount)
          val bytesPerSample = bandType.bytesPerSample
          val bytesPerCell = bytesPerSample * bandCount

          cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
            val segment = getSegment(segmentIndex).bytes
            val segmentSize = segment.size
            val bandSegmentCount = segmentSize / bandCount
            val bandSegment = Array.ofDim[Byte](bandSegmentCount)

            var j = 0
            cfor(bandIndex * bytesPerSample)(_ < segmentSize, _ + bytesPerCell) { i =>
              var b = 0
              while(b < bytesPerSample) { bandSegment(j + b) = segment(i + b) ; b += 1 }
              j += bytesPerSample
            }

            compressedBandBytes(segmentIndex) = compressor.compress(bandSegment, segmentIndex)
          }

          GeoTiffTile(bandType, compressedBandBytes, compressor.createDecompressor(), segmentLayout, compression)
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val compressedBandBytes = Array.ofDim[Array[Byte]](bandSegmentCount)

      var start = bandSegmentCount * bandIndex
      cfor(0)(_ < bandSegmentCount, _ + 1) { i =>
        compressedBandBytes(i) = compressedBytes(i + start).clone
      }

      GeoTiffTile(bandType, compressedBandBytes, decompressor, segmentLayout, compression)
    }
  }

  def toArrayTile(): ArrayMultiBandTile =
    ArrayMultiBandTile((0 until bandCount map { band(_).toArrayTile }):_*)

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

  def mapSegments(f: (GeoTiffSegment, Int) => Array[Byte]): MultiBandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)

    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      arr(segmentIndex) = compressor.compress(f(getSegment(segmentIndex), segmentIndex), segmentIndex)
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

  def map(b0: Int)(f: Int => Int): MultiBandTile =
    if(hasPixelInterleave) {
      mapSegments { (segment, _) =>
        segment.mapWithIndex { (i, z) =>
          if(i % bandCount == b0)
            f(z)
          else
            z
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val start =  bandSegmentCount * b0

      mapSegments { (segment, segmentIndex) =>
        if(start <= segmentIndex && segmentIndex < start + bandSegmentCount) {
          segment.map(f)
        } else {
          segment.bytes
        }
      }
    }

  def mapDouble(b0: Int)(f: Double => Double): MultiBandTile =
    if(hasPixelInterleave) {
      mapSegments { (segment, _) =>
        segment.mapDoubleWithIndex { (i, z) =>
          if(i % bandCount == b0)
            f(z)
          else
            z
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val start =  bandSegmentCount * b0

      mapSegments { (segment, segmentIndex) =>
        if(start <= segmentIndex && segmentIndex < start + bandSegmentCount) {
          segment.mapDouble(f)
        } else {
          segment.bytes
        }
      }
    }

  def map(f: (Int, Int) => Int): MultiBandTile = {
    if(hasPixelInterleave) {
      mapSegments { (segment, semgentIndex) =>
        segment.mapWithIndex { (i, z) =>
          f(i % bandCount, z)
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      mapSegments { (segment, segmentIndex) =>
        val bandIndex = segmentIndex / bandSegmentCount
        segment.map { z => f(bandIndex, z) }
      }
    }
  }

  def mapDouble(f: (Int, Double) => Double): MultiBandTile = {
    if(hasPixelInterleave) {
      mapSegments { (segment, semgentIndex) =>
        segment.mapDoubleWithIndex { (i, z) =>
          f(i % bandCount, z)
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      mapSegments { (segment, segmentIndex) =>
        val bandIndex = segmentIndex / bandSegmentCount
        segment.mapDouble { z => f(bandIndex, z) }
      }
    }
  }

  def foreach(b0: Int)(f: Int => Unit): Unit =
    _foreach(b0) { (segment, i) => f(segment.getInt(i)) }

  def foreachDouble(b0: Int)(f: Double => Unit): Unit =
    _foreach(b0) { (segment, i) => f(segment.getDouble(i)) }

  private def _foreach(b0: Int)(f: (GeoTiffSegment, Int) => Unit): Unit = {
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
                f(segment, i)
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
              f(segment, i)
            }
          }
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val start =  bandSegmentCount * b0

      if(isTiled) {
        cfor(start)(_ < start + bandSegmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex % bandSegmentCount)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(segment, i)
            }
          }
        }
      } else {
        cfor(start)(_ < start + bandSegmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(segment, i)
          }
        }
      }
    }
  }

  def foreach(f: (Int, Int) => Unit): Unit =
    _foreach { (segmentIndex, segment, i) =>
      f(segmentIndex, segment.getInt(i))
    }

  def foreachDouble(f: (Int, Double) => Unit): Unit =
    _foreach { (segmentIndex, segment, i) =>
      f(segmentIndex, segment.getDouble(i))
    }

  private def _foreach(f: (Int, GeoTiffSegment, Int) => Unit): Unit = {
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
              f(i % bandCount, segment, i)
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(i % bandCount, segment, i)
          }
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      if(isTiled) {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex % bandSegmentCount)
          val bandIndex = segmentIndex / bandSegmentCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(bandIndex, segment, i)
            }
          }
        }
      } else {
        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val bandIndex = segmentIndex / bandSegmentCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(bandIndex, segment, i)
          }
        }
      }
    }
  }

  override
  def combine(f: Array[Int] => Int): Tile =
    _combine(_.initValueHolder)({ segmentCombiner => segmentCombiner.placeValue _ })({ segmentCombiner =>
      { i => segmentCombiner.setFromValues(i, f) }
    })

  override
  def combineDouble(f: Array[Double] => Double): Tile = 
    _combine(_.initValueHolderDouble)({ segmentCombiner => segmentCombiner.placeValueDouble _ })({ segmentCombiner => 
      { i => segmentCombiner.setFromValuesDouble(i, f) }
    })

  private def _combine(initValueHolder: SegmentCombiner => Unit)(placeValue: SegmentCombiner => (GeoTiffSegment, Int, Int) => Unit)(setFromValues: SegmentCombiner => (Int) => Unit): Tile = {
    val (arr, compressor) =
      if(hasPixelInterleave) {
        val compressor = compression.createCompressor(segmentCount)
        val arr = Array.ofDim[Array[Byte]](segmentCount)

        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size

          val segmentCombiner = createSegmentCombiner(segmentSize / bandCount)
          initValueHolder(segmentCombiner)

          var j = 0
          cfor(0)(_ < segmentSize, _ + bandCount) { i =>
            cfor(0)(_ < bandCount, _ + 1) { j =>
              placeValue(segmentCombiner)(segment, i + j, j)
            }
            setFromValues(segmentCombiner)(j)
            j += 1
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      } else {
        // This path will be especially slow, since we need to load up each segment sequentially for each cell
        val bandSegmentCount = segmentCount / bandCount
        val compressor = compression.createCompressor(bandSegmentCount)
        val arr = Array.ofDim[Array[Byte]](bandSegmentCount)

        cfor(0)(_ < bandSegmentCount, _ + 1) { segmentIndex =>
          val segmentSize = getSegment(segmentIndex).size
          val segmentCombiner = createSegmentCombiner(segmentSize)
          initValueHolder(segmentCombiner)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
              val segment = getSegment(bandIndex * bandSegmentCount + segmentIndex)
              placeValue(segmentCombiner)(segment, i, bandIndex)
            }
            setFromValues(segmentCombiner)(i)
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      }

    GeoTiffTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def combine(b0: Int,b1: Int)(f: (Int, Int) => Int): Tile =
    _combine(b0: Int, b1: Int) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int) =>
        segmentCombiner.set(targetIndex, s1, i1, s2, i2)(f) 
      }
    }

  def combineDouble(b0: Int,b1: Int)(f: (Double, Double) => Double): Tile =
    _combine(b0: Int, b1: Int) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int) =>
        segmentCombiner.setDouble(targetIndex, s1, i1, s2, i2)(f) 
      }
    }

  private def _combine(b0: Int,b1: Int)(set: SegmentCombiner => (Int, GeoTiffSegment, Int, GeoTiffSegment, Int) => Unit): Tile = {
    assert(b0 < bandCount, s"Illegal band index: $b0 is out of range ($bandCount bands)")
    assert(b1 < bandCount, s"Illegal band index: $b1 is out of range ($bandCount bands)")
    val (arr, compressor) =
      if(hasPixelInterleave) {
        val diff = b1 - b0

        val compressor = compression.createCompressor(segmentCount)
        val arr = Array.ofDim[Array[Byte]](segmentCount)

        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentCombiner = createSegmentCombiner(segmentSize / bandCount)

          var j = 0
          cfor(b0)(_ < segmentSize, _ + bandCount) { i =>
            set(segmentCombiner)(j, segment, i, segment, i + diff)
            j += 1
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }
        (arr, compressor)
      } else {
        val bandSegmentCount = segmentCount / bandCount
        val compressor = compression.createCompressor(bandSegmentCount)
        val arr = Array.ofDim[Array[Byte]](bandSegmentCount)

        val start0 = bandSegmentCount * b0
        val start1 = bandSegmentCount * b1
        cfor(0)(_ < bandSegmentCount, _ + 1) { segmentIndex =>
          val segment0 = getSegment(start0 + segmentIndex)
          val segment1 = getSegment(start1 + segmentIndex)
          val segmentSize0 = segment0.size
          val segmentSize1 = segment1.size
          assert(segmentSize0 == segmentSize1, "GeoTiff band segments do not match in size!")
          val segmentCombiner = createSegmentCombiner(segmentSize0)

          cfor(0)(_ < segmentSize0, _ + 1) { i =>
            set(segmentCombiner)(i, segment0, i, segment1, i)
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      }

    GeoTiffTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  override
  def combineIntTileCombiner(combiner: IntTileCombiner3): Tile =
    _combine(combiner.b0, combiner.b1, combiner.b2) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int) =>
        segmentCombiner.set(targetIndex, s1, i1, s2, i2, s3, i3)(combiner)
      }
    }

  override
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner3): Tile =
    _combine(combiner.b0, combiner.b1, combiner.b2) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int) =>
        segmentCombiner.setDouble(targetIndex, s1, i1, s2, i2, s3, i3)(combiner)
      }
    }

  private def _combine(b0: Int, b1: Int, b2: Int)
    (set: SegmentCombiner => (Int, GeoTiffSegment, Int, GeoTiffSegment, Int, GeoTiffSegment, Int) => Unit): Tile = {
    assert(b0 < bandCount, s"Illegal band index: $b0 is out of range ($bandCount bands)")
    assert(b1 < bandCount, s"Illegal band index: $b1 is out of range ($bandCount bands)")
    assert(b2 < bandCount, s"Illegal band index: $b2 is out of range ($bandCount bands)")

    val (arr, compressor) =
      if(hasPixelInterleave) {
        val diff1 = b1 - b0
        val diff2 = b2 - b0

        val compressor = compression.createCompressor(segmentCount)
        val arr = Array.ofDim[Array[Byte]](segmentCount)

        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentCombiner = createSegmentCombiner(segmentSize / bandCount)

          var j = 0
          cfor(b0)(_ < segmentSize, _ + bandCount) { i =>
            set(segmentCombiner)(j, segment, i, segment, i + diff1, segment, i + diff2)
            j += 1
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }
        (arr, compressor)
      } else {
        val bandSegmentCount = segmentCount / bandCount
        val compressor = compression.createCompressor(bandSegmentCount)
        val arr = Array.ofDim[Array[Byte]](bandSegmentCount)

        val start0 = bandSegmentCount * b0
        val start1 = bandSegmentCount * b1
        val start2 = bandSegmentCount * b2
        cfor(0)(_ < bandSegmentCount, _ + 1) { segmentIndex =>
          val segment0 = getSegment(start0 + segmentIndex)
          val segment1 = getSegment(start1 + segmentIndex)
          val segment2 = getSegment(start2 + segmentIndex)
          val segmentSize = segment0.size

          val segmentCombiner = createSegmentCombiner(segmentSize)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            set(segmentCombiner)(i, segment0, i, segment1, i, segment2, i)
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      }

    GeoTiffTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  def combineIntTileCombiner(combiner: IntTileCombiner4): Tile =
    _combine(combiner.b0, combiner.b1, combiner.b2, combiner.b3) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int, s4: GeoTiffSegment, i4: Int) =>
        segmentCombiner.set(targetIndex, s1, i1, s2, i2, s3, i3, s4, i4)(combiner)
      }
    }

  def combineDoubleTileCombiner(combiner: DoubleTileCombiner4): Tile =
    _combine(combiner.b0, combiner.b1, combiner.b2, combiner.b3) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int, s4: GeoTiffSegment, i4: Int) =>
        segmentCombiner.setDouble(targetIndex, s1, i1, s2, i2, s3, i3, s4, i4)(combiner)
      }
    }

  private def _combine(b0: Int, b1: Int, b2: Int, b3: Int)
    (set: SegmentCombiner => (Int, GeoTiffSegment, Int, GeoTiffSegment, Int, GeoTiffSegment, Int, GeoTiffSegment, Int) => Unit): Tile = {
    assert(b0 < bandCount, s"Illegal band index: $b0 is out of range ($bandCount bands)")
    assert(b1 < bandCount, s"Illegal band index: $b1 is out of range ($bandCount bands)")
    assert(b2 < bandCount, s"Illegal band index: $b2 is out of range ($bandCount bands)")
    assert(b3 < bandCount, s"Illegal band index: $b2 is out of range ($bandCount bands)")

    val (arr, compressor) =
      if(hasPixelInterleave) {
        val diff1 = b1 - b0
        val diff2 = b2 - b0
        val diff3 = b3 - b0

        val compressor = compression.createCompressor(segmentCount)
        val arr = Array.ofDim[Array[Byte]](segmentCount)

        cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
          val segment = getSegment(segmentIndex)
          val segmentSize = segment.size
          val segmentCombiner = createSegmentCombiner(segmentSize / bandCount)

          var j = 0
          cfor(b0)(_ < segmentSize, _ + bandCount) { i =>
            set(segmentCombiner)(j, segment, i, segment, i + diff1, segment, i + diff2, segment, i + diff3)
            j += 1
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }
        (arr, compressor)
      } else {
        val bandSegmentCount = segmentCount / bandCount
        val compressor = compression.createCompressor(bandSegmentCount)
        val arr = Array.ofDim[Array[Byte]](bandSegmentCount)

        val start0 = bandSegmentCount * b0
        val start1 = bandSegmentCount * b1
        val start2 = bandSegmentCount * b2
        val start3 = bandSegmentCount * b3
        cfor(0)(_ < bandSegmentCount, _ + 1) { segmentIndex =>
          val segment0 = getSegment(start0 + segmentIndex)
          val segment1 = getSegment(start1 + segmentIndex)
          val segment2 = getSegment(start2 + segmentIndex)
          val segment3 = getSegment(start3 + segmentIndex)
          val segmentSize = segment0.size

          val segmentCombiner = createSegmentCombiner(segmentSize)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            set(segmentCombiner)(i, segment0, i, segment1, i, segment2, i, segment3, i)
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      }

    GeoTiffTile(
      BandType.forCellType(cellType),
      arr,
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      None
    )
  }

  /** Creates a segment combiner, which is an abstraction that allows us to generalize
    * the combine algorithms over BandType. */
  protected def createSegmentCombiner(targetSize: Int): SegmentCombiner

  /** This trait is how subclasses define the necessary pieces that allow
    * us to abstract over each of the combine functions */
  protected abstract class SegmentCombiner {

    private var valueHolder: Array[Int] = null
    private var valueHolderDouble: Array[Double] = null

    def initValueHolder(): Unit = { valueHolder = Array.ofDim[Int](bandCount) }
    def initValueHolderDouble(): Unit = { valueHolderDouble = Array.ofDim[Double](bandCount) }

    def set(targetIndex: Int, v: Int): Unit
    def setDouble(targetIndex: Int, v: Double): Unit

    def set(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int)
      (f: (Int, Int) => Int): Unit = {
      val z1 = s1.getInt(i1)
      val z2 = s2.getInt(i2)
      set(targetIndex, f(z1, z2))
    }

    def set(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int)
      (combiner: IntTileCombiner3): Unit = {
      val z1 = s1.getInt(i1)
      val z2 = s2.getInt(i2)
      val z3 = s3.getInt(i3)
      set(targetIndex, combiner(z1, z2, z3))
    }

    def set(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int, s4: GeoTiffSegment, i4: Int)
      (combiner: IntTileCombiner4): Unit = {
      val z1 = s1.getInt(i1)
      val z2 = s2.getInt(i2)
      val z3 = s3.getInt(i3)
      val z4 = s4.getInt(i4)
      set(targetIndex, combiner(z1, z2, z3, z4))
    }

    def setDouble(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int)
      (f: (Double, Double) => Double): Unit = {
      val z1 = s1.getDouble(i1)
      val z2 = s2.getDouble(i2)
      setDouble(targetIndex, f(z1, z2))
    }

    def setDouble(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int)
      (combiner: DoubleTileCombiner3): Unit = {
      val z1 = s1.getInt(i1)
      val z2 = s2.getInt(i2)
      val z3 = s3.getInt(i3)
      setDouble(targetIndex, combiner(z1, z2, z3))
    }

    def setDouble(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int, s3: GeoTiffSegment, i3: Int, s4: GeoTiffSegment, i4: Int)
      (combiner: DoubleTileCombiner4): Unit = {
      val z1 = s1.getDouble(i1)
      val z2 = s2.getDouble(i2)
      val z3 = s3.getDouble(i3)
      val z4 = s4.getDouble(i4)
      setDouble(targetIndex, combiner(z1, z2, z3, z4))
    }

    // Used for combining all bands.
    def placeValue(segment: GeoTiffSegment, i: Int, bandIndex: Int): Unit = {
      valueHolder(bandIndex) = segment.getInt(i)
    }

    def setFromValues(targetIndex: Int, f: Array[Int] => Int): Unit = {
      set(targetIndex, f(valueHolder))
    }

    def placeValueDouble(segment: GeoTiffSegment, i: Int, bandIndex: Int): Unit = {
      valueHolderDouble(bandIndex) = segment.getDouble(i)
    }

    def setFromValuesDouble(targetIndex: Int, f: Array[Double] => Double): Unit = {
      setDouble(targetIndex, f(valueHolderDouble))
    }

    def getBytes(): Array[Byte]
  }
}
