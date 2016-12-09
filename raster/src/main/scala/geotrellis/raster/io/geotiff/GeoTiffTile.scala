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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.compression._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.split._
import geotrellis.vector.Extent

import java.util.BitSet

import spire.syntax.cfor._

object GeoTiffTile {
  /**
   * Creates a new instance of GeoTiffTile.
   *
   * @param compressedBytes: An Array[Array[Byte]] that represents the segments in the GeoTiff
   * @param decompressor: A [[Decompressor]] for the given data compression
   * @param segmentLayout: The [[GeoTiffSegmentLayout]] of the GeoTiff
   * @param compresson: The [[Compression]] type of the data
   * @param cellType: The [[CellType]] of the segments
   * @param bandType: The data storage format of the band. Defaults to None
   * @return A new instance of GeoTiffTile based on the given bandType or cellType
   */
  def apply(
    segmentBytes: SegmentBytes,
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    compression: Compression,
    cellType: CellType,
    bandType: Option[BandType] = None
  ): GeoTiffTile = {
    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          // Bytes
          case ct: ByteCells =>
            new ByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          // UBytes
          case ct: UByteCells =>
            new UByteGeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          // Shorts
          case ct: ShortCells =>
            new Int16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          // UShorts
          case ct: UShortCells =>
            new UInt16GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          case ct: IntCells =>
            new Int32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          case ct: FloatCells =>
            new Float32GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
          case ct: DoubleCells =>
            new Float64GeoTiffTile(segmentBytes, decompressor, segmentLayout, compression, ct)
        }
      }
    }

  /** Convert a tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. */
  def apply(tile: Tile): GeoTiffTile =
    apply(tile, GeoTiffOptions.DEFAULT)

  def apply(tile: Tile, options: GeoTiffOptions): GeoTiffTile = {
    val bandType = BandType.forCellType(tile.cellType)

    val segmentLayout = GeoTiffSegmentLayout(tile.cols, tile.rows, options.storageMethod, bandType)

    val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
    val compressor = options.compression.createCompressor(segmentCount)

    val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)
    val segmentTiles =
      options.storageMethod match {
        case _: Tiled => tile.split(segmentLayout.tileLayout)
        case _: Striped => tile.split(segmentLayout.tileLayout, Split.Options(extend = false))
      }

    cfor(0)(_ < segmentCount, _ + 1) { i =>
      val bytes = segmentTiles(i).toBytes
      segmentBytes(i) = compressor.compress(bytes, i)
    }

    apply(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, tile.cellType)
  }

  // TODO: iterate one segment at a time
  type SegmentSource = Seq[Int] => Seq[Array[Byte]]

  def cropSegments(
    segmentLayout: GeoTiffSegmentLayout,
    segmentSource: SegmentSource,
    compression: Compression,
    bounds: GridBounds
  ): (GeoTiffSegmentLayout, Array[Array[Byte]]) = { // Maybe return GeoTiffTile?
    val targetLayout = segmentLayout.crop(bounds)
    val overlap: Array[Int] = segmentLayout.intersectingSegments(bounds)

    // TODO: Figure out how to iterate over the overlapping segments efficiently
    // use the grid bounds of each target relative to source layout to find all intersecting segments.// iterate over the pixels in the overlap

    ???
  }
}

abstract class GeoTiffTile(
  val segmentLayout: GeoTiffSegmentLayout,
  compression: Compression // Compression to use moving forward
) extends Tile with GeoTiffImageData {
  val cellType: CellType

  val bandCount = 1

  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  private val isTiled = segmentLayout.isTiled

  /**
   * Converts the CellType of the GeoTiffTile to the
   * given CellType
   *
   * @param newCellType: The [[CellType]] to be converted to
   * @return A new [[Tile]] that contains the new CellTypes
   */
  def convert(newCellType: CellType): GeoTiffTile = {
    if(newCellType.isFloatingPoint != cellType.isFloatingPoint)
      logger.warn(s"Conversion from $cellType to $newCellType may lead to data loss.")

    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.convert(newCellType)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      newCellType
    )
  }

  val segmentCount = segmentBytes.size

  /**
   * Returns the GeoTiffSegment of the corresponding index
   *
   * @param i: The index of the segment
   * @return The corresponding [[GeoTiffSegment]]
   */
  def getSegment(i: Int): GeoTiffSegment

  /**
   * Given a col and row, find the segment where this point resides.
   *
   * @param col: The col number
   * @param row: The row number
   * @return An Int that represents the segment's index
   */
  def get(col: Int, row: Int): Int = {
    val segmentIndex = segmentLayout.getSegmentIndex(col, row)
    val i =
      if (segmentLayout.isStriped)
        StripedSegmentTransform(segmentIndex, segmentLayout).gridToIndex(col, row)
      else
        TiledSegmentTransform(segmentIndex, segmentLayout).gridToIndex(col, row)

    getSegment(segmentIndex).getInt(i)
  }

  /**
   * Given a col and row, find the segment that this point is within.
   *
   * @param col: The col number
   * @param row: The row number
   * @return A Double that represents the segment's index
   */
  def getDouble(col: Int, row: Int): Double = {
    val segmentIndex = segmentLayout.getSegmentIndex(col, row)
    val i =
      if (segmentLayout.isStriped)
        StripedSegmentTransform(segmentIndex, segmentLayout).gridToIndex(col, row)
      else
        TiledSegmentTransform(segmentIndex, segmentLayout).gridToIndex(col, row)

    getSegment(segmentIndex).getDouble(i)
  }

  /**
   * Takes a function that takes an Int and returns a Unit for each
   * segment in the GeoTiffTile.
   *
   * @param f: A function that takes an Int and returns a Unit
   * @return A Unit for each segment in the GeoTiffTile
   */
  def foreach(f: Int => Unit): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size

      if(isTiled) {
        // Need to check for bounds
        val segmentTransform = TiledSegmentTransform(segmentIndex, segmentLayout)
        cfor(0)(_ < segmentSize, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if(col < cols && row < rows) {
            f(segment.getInt(i))
          }
        }
      } else {
        cfor(0)(_ < segmentSize, _ + 1) { i =>
          f(segment.getInt(i))
        }
      }
    }
  }

  /**
   * Takes a function that takes a Double and returns a Unit for each
   * segment in the GeoTiffTile.
   *
   * @param f: A function that takes a Double and returns a Unit
   * @return A Unit for each segment in the GeoTiffTile
   */
  def foreachDouble(f: Double => Unit): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size

      if(isTiled) {
        // Need to check for bounds
        val segmentTransform = TiledSegmentTransform(segmentIndex, segmentLayout)
        cfor(0)(_ < segmentSize, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if(col < cols && row < rows) {
            f(segment.getDouble(i))
          }
        }
      } else {
        cfor(0)(_ < segmentSize, _ + 1) { i =>
          f(segment.getDouble(i))
        }
      }
    }
  }

  /**
   * Takes a function that takes an Int and returns an Int on each
   * segment in the GeoTiffTile.
   *
   * @param f: A function that takes an Int and returns an Int
   * @return A [[GeoTiffTile]] that contains the newly mapped values
   */
  def map(f: Int => Int): GeoTiffTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.map(f(_))
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType
    )
  }

  /**
   * Takes a function that takes a Double and returns a Double on each
   * segment in the GeoTiffTile.
   *
   * @param f: A function that takes a Double and returns a Double
   * @return A [[GeoTiffTile]] that contains the newly mapped values
   */
  def mapDouble(f: Double => Double): GeoTiffTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val newBytes = segment.mapDouble(f)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType
    )
  }

  /**
   * Executes an [[IntTileVisitor]] at each cell of the GeoTiffTile.
   *
   * @param visitor: An IntTileVisitor
   */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size

      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(segmentIndex, segmentLayout)
        else
          TiledSegmentTransform(segmentIndex, segmentLayout)

      cfor(0)(_ < segmentSize, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          visitor(col, row, segment.getInt(i))
        }
      }
    }
  }

  /**
   * Executes a [[DoubleTileVisitor]] at each cell of the GeoTiffTile.
   *
   * @param visitor: An DoubleTileVisitor
   */
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentSize = segment.size
      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(segmentIndex, segmentLayout)
        else
          TiledSegmentTransform(segmentIndex, segmentLayout)
      cfor(0)(_ < segmentSize, _ + 1) { i =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          visitor(col, row, segment.getDouble(i))
        }
      }
    }
  }

  /**
   * Map an [[IntTileMapper]] over the given tile.
   *
   * @param mapper: The IntTileMapper
   * @return A [[Tile]] with the results of the mapper
   */
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(segmentIndex, segmentLayout)
        else
          TiledSegmentTransform(segmentIndex, segmentLayout)

      val newBytes = segment.mapWithIndex { (i, z) =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          mapper(col, row, z)
        } else { 0 }

      }
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType
    )
  }

  /**
   * Map a [[DoubleTileMapper]] over the given tile.
   *
   * @param mapper: The DoubleTileMapper
   * @return A [[Tile]] with the results of the mapper
   */
  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
      val segment = getSegment(segmentIndex)
      val segmentTransform =
        if (segmentLayout.isStriped)
          StripedSegmentTransform(segmentIndex, segmentLayout)
        else
          TiledSegmentTransform(segmentIndex, segmentLayout)
      val newBytes = segment.mapDoubleWithIndex { (i, z) =>
        val col = segmentTransform.indexToCol(i)
        val row = segmentTransform.indexToRow(i)
        if(col < cols && row < rows) {
          mapper(col, row, z)
        } else { 0.0 }
      }
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType
    )
  }

  /**
   * Combines two GeoTiffTiles by applying a function
   * to both and using the result to create a new Tile.
   *
   * @param other: The [[Tile]] to be combined with
   * @param f: A function that takes (Int, Int) and returns an Int
   * @return A [[Tile]] that contains the results of the given function
   */
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
          new ArraySegmentBytes(arr),
          compressor.createDecompressor(),
          segmentLayout,
          compression,
          cellType
        )
      case _ =>
        this.map { (col, row, z) =>
          f(z, other.get(col, row))
        }
    }

  /**
   * Combines two GeoTiffTiles by applying a function
   * to both and using the result to create a new Tile.
   *
   * @param other: The [[Tile]] to be combined with
   * @param f: A function that takes (Double, Double) and returns a Double
   * @return A [[Tile]] that contains the results of the given function
   */
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
          new ArraySegmentBytes(arr),
          compressor.createDecompressor(),
          segmentLayout,
          compression,
          cellType
        )
      case _ =>
        this.mapDouble { (col, row, z) =>
          f(z, other.get(col, row))
        }
    }

  /**
   * Converts the given implementation to an Array
   *
   * @return An Array[Int] that conatains all of the values in the tile
   */
  def toArray(): Array[Int] =
    toArrayTile.toArray

  /**
   * Converts the given implementation to an Array
   *
   * @return An Array[Double] that conatains all of the values in the tile
   */
  def toArrayDouble(): Array[Double] =
    toArrayTile.toArrayDouble

  /**
   * Converts GeoTiffTile to an ArrayTile
   *
   * @return An [[ArrayTile]] of the GeoTiffTile
   */
  def toArrayTile(): ArrayTile = mutable

  /**
   * Reads in the bytes of the segments of this [[GeoTiffTile]] and returns
   * them as an Array[Byte]. This method is for tiles who's segments are stripped.
   * Both cropped and full readings can use this method to retrieve the desired bytes.
   *
   * @param segmentBytes: The [[SegmentBytes]] of the tile.
   * @param segmentLayout: The [[SegmentLayout]] of the tile.
   * @param bytesPerPixel: The ConstantNoDataCellType byte size of the tile.
   * @param windowBounds: The gridBounds of the area that is to be read in.
   *
   * @returns An Array[Byte] that contains the data from the selected region.
   */
  def readStrippedSegmentBytes(
    segmentBytes: SegmentBytes,
    segmentLayout: GeoTiffSegmentLayout,
    bytesPerPixel: Int,
    windowBounds: GridBounds
  ): Array[Byte] = {
    val arr = Array.ofDim[Byte](windowBounds.size * bytesPerPixel)
    var counter: Int = 0

    if (windowBounds == gridBounds) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)
        val segmentLength = segment.bytes.length
        System.arraycopy(segment.bytes, 0, arr, counter, segmentLength)
        counter += segmentLength
      }
    } else {
      val segmentIds = segmentBytes.intersectingSegments
      cfor(0)(_ < segmentIds.length, _ + 1) { i =>
        val segmentId = segmentIds(i)
        val segmentGridBounds = segmentLayout.getGridBounds(segmentId)
        val segment: GeoTiffSegment = getSegment(segmentId)

        val result = windowBounds.intersection(segmentGridBounds).get
        val intersection = Intersection(segmentGridBounds, result, segmentLayout)

        val adjStart = intersection.start * bytesPerPixel
        val adjEnd = intersection.end * bytesPerPixel
        val adjCols = cols * bytesPerPixel
        val adjWidth = result.width * bytesPerPixel

        cfor(adjStart)(_ < adjEnd, _ + adjCols) { i =>
          System.arraycopy(segment.bytes, i - adjStart, arr, counter, adjWidth)
          counter += adjWidth
        }
      }
    }
    arr
  }

  /**
   * Reads in the bytes of the segments of this [[GeoTiffTile]] and returns
   * them as an Array[Byte]. This method is for tiles who's segments are tiled.
   * Both cropped and full readings can use this method to retrieve the desired bytes.
   *
   * @param segmentBytes: The [[SegmentBytes]] of the tile.
   * @param segmentLayout: The [[SegmentLayout]] of the tile.
   * @param bytesPerPixel: The ConstantNoDataCellType byte size of the tile.
   * @param windowBounds: The gridBounds of the area that is to be read in.
   *
   * @returns An Array[Byte] that contains the data from the selected region.
   */
  def readTiledSegmentBytes(
    segmentBytes: SegmentBytes,
    segmentLayout: GeoTiffSegmentLayout,
    bytesPerPixel: Int,
    windowBounds: GridBounds
  ): Array[Byte] = {
    val arr = Array.ofDim[Byte](windowBounds.size * bytesPerPixel)

    if (windowBounds == gridBounds) {
      cfor(0)(_ < segmentCount, _ + 1) { segmentIndex =>
        val segment = getSegment(segmentIndex)

        val segmentTransform = TiledSegmentTransform(segmentIndex, segmentLayout)
        val width = segmentTransform.segmentCols * bytesPerPixel
        val tileWidth = segmentLayout.tileLayout.tileCols * bytesPerPixel

        cfor(0)(_ < tileWidth * segmentTransform.segmentRows, _ + tileWidth) { i =>
          val col = segmentTransform.indexToCol(i / bytesPerPixel)
          val row = segmentTransform.indexToRow(i / bytesPerPixel)
          val j = ((row * cols) + col) * bytesPerPixel
          System.arraycopy(segment.bytes, i, arr, j, width)
        }
      }
    } else {
      val segmentIds = segmentBytes.intersectingSegments
      cfor(0)(_ < segmentIds.length, _ + 1) { i =>
        val segmentId = segmentIds(i)
        val segmentGridBounds = segmentLayout.getGridBounds(segmentId)
        val segment: GeoTiffSegment = getSegment(segmentId)
        val segmentTransform = TiledSegmentTransform(segmentId, segmentLayout)

        val result = windowBounds.intersection(segmentGridBounds).get
        val intersection = Intersection(segmentGridBounds, result, segmentLayout)

        val adjStart = intersection.start * bytesPerPixel
        val adjEnd = intersection.end * bytesPerPixel
        val adjWidth = result.width * bytesPerPixel
        val adjTileWidth = intersection.tileWidth * bytesPerPixel

        cfor(adjStart)(_ < adjEnd, _ + adjTileWidth) { i =>
          val col = segmentTransform.indexToCol(i / bytesPerPixel)
          val row = segmentTransform.indexToRow(i / bytesPerPixel)
          if (gridBounds.contains(col, row)) {
            val j = (row - windowBounds.rowMin) * windowBounds.width + (col - windowBounds.colMin)
            System.arraycopy(segment.bytes, i, arr, j * bytesPerPixel, adjWidth)
          }
        }
      }
    }
    arr
  }

  /**
   * Converts GeoTiffTile to a MutableArrayTile
   *
   * @return A [[MutableArrayTile]] of the GeoTiffTile
   */
  def mutable: MutableArrayTile

  /**
   * Crop this tile to given pixel region.
   *
   * @param bounds: Pixel bounds specifying the crop area.
   * @return A [[MutableArrayTile]] of the cropped region
   */
  def crop(bounds: GridBounds): MutableArrayTile = {
    val tile = ArrayTile.empty(cellType, bounds.width, bounds.height)

    // TODO: iterate in offset order, not in TiffTags order
    for (segmentId <- segmentBytes.intersectingSegments) {
      val segment = getSegment(segmentId)
      val segmentBounds = segmentLayout.getGridBounds(segmentId)
      val segmentTransform = segmentLayout.getSegmentTransform(segmentId)
      val overlap = bounds.intersection(segmentBounds).get

      if (tile.cellType.isFloatingPoint) {
        cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
          cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
            val i = segmentTransform.gridToIndex(col, row)
            val v = segment.getInt(i)
            tile.set(col - bounds.colMin, row - bounds.rowMin, v)
          }
        }
      } else {
        cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
          cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
            val i = segmentTransform.gridToIndex(col, row)
            val v = segment.getDouble(i)
            tile.setDouble(col - bounds.colMin, row - bounds.rowMin, v)
          }
        }
      }
    }

    tile
  }

  /**
   * Converts the GeoTiffTile to an Array[Byte]
   *
   * @return An Array[Byte] of the GeoTiffTile
   */
  def toBytes(): Array[Byte] =
    toArrayTile.toBytes
}
