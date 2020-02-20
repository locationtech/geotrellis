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
   * @param segmentBytes: An Array[Array[Byte]] that represents the segments in the GeoTiff
   * @param decompressor: A [[Decompressor]] for the given data compression
   * @param segmentLayout: The [[GeoTiffSegmentLayout]] of the GeoTiff
   * @param compression: The [[Compression]] type of the data
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
    bandType: Option[BandType] = None,
    overviews: List[GeoTiffTile] = Nil
  ): GeoTiffTile = {
    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt32GeoTiffTile => gt }
            )
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: BitGeoTiffTile => gt }
            )
          // Bytes
          case ct: ByteCells =>
            new ByteGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: ByteGeoTiffTile => gt }
            )
          // UBytes
          case ct: UByteCells =>
            new UByteGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UByteGeoTiffTile => gt }
            )
          // Shorts
          case ct: ShortCells =>
            new Int16GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int16GeoTiffTile => gt }
            )
          // UShorts
          case ct: UShortCells =>
            new UInt16GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt16GeoTiffTile => gt }
            )
          case ct: IntCells =>
            new Int32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int32GeoTiffTile => gt }
            )
          case ct: FloatCells =>
            new Float32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float32GeoTiffTile => gt }
            )
          case ct: DoubleCells =>
            new Float64GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float64GeoTiffTile => gt }
            )
        }
      }
    }

  def applyOverview(
    geoTiffTile: GeoTiffTile,
    compression: Compression,
    cellType: CellType,
    bandType: Option[BandType] = None
  ): GeoTiffTile = {
    val segmentCount = geoTiffTile.segmentCount
    val segmentBytes = geoTiffTile.segmentBytes
    val segmentLayout = geoTiffTile.segmentLayout
    val compressor = compression.createCompressor(segmentCount)
    val decompressor = compressor.createDecompressor()
    val overviews = geoTiffTile.overviews.map(applyOverview(_, compression, cellType, bandType))

    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt32GeoTiffTile => gt }
            )
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: BitGeoTiffTile => gt }
            )
          // Bytes
          case ct: ByteCells =>
            new ByteGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: ByteGeoTiffTile => gt }
            )
          // UBytes
          case ct: UByteCells =>
            new UByteGeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UByteGeoTiffTile => gt }
            )
          // Shorts
          case ct: ShortCells =>
            new Int16GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int16GeoTiffTile => gt }
            )
          // UShorts
          case ct: UShortCells =>
            new UInt16GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt16GeoTiffTile => gt }
            )
          case ct: IntCells =>
            new Int32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int32GeoTiffTile => gt }
            )
          case ct: FloatCells =>
            new Float32GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float32GeoTiffTile => gt }
            )
          case ct: DoubleCells =>
            new Float64GeoTiffTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float64GeoTiffTile => gt }
            )
        }
    }
  }

  /** Convert a tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. */
  def apply(tile: Tile): GeoTiffTile =
    apply(tile, GeoTiffOptions.DEFAULT)

  def apply(tile: Tile, options: GeoTiffOptions): GeoTiffTile = {
    val bandType = BandType.forCellType(tile.cellType)

    val segmentLayout = GeoTiffSegmentLayout(tile.cols, tile.rows, options.storageMethod, BandInterleave, bandType)

    val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
    val compressor = options.compression.createCompressor(segmentCount)

    val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)
    val segmentTiles: Seq[Tile] =
      options.storageMethod match {
        case _: Tiled => tile.split(segmentLayout.tileLayout)
        case _: Striped => tile.split(segmentLayout.tileLayout, Split.Options(extend = false))
      }

    cfor(0)(_ < segmentCount, _ + 1) { i =>
      val bytes = segmentTiles(i).toBytes
      segmentBytes(i) = compressor.compress(bytes, i)
    }

    // Our BitCellType rasters have the bits encoded in a order inside of each byte that is
    // the reverse of what a GeoTiff wants.
    if(tile.cellType == BitCellType) {
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        cfor(0)(_ < segmentBytes(i).length, _ + 1) { j =>
          segmentBytes(i)(j) = ((Integer.reverse(segmentBytes(i)(j)) >>> 24) & 0xFF).toByte
        }
      }
    }

    apply(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, tile.cellType)
  }
}

abstract class GeoTiffTile(
  val segmentLayout: GeoTiffSegmentLayout,
  compression: Compression, // Compression to use moving forward
  val overviews: List[GeoTiffTile] = Nil
) extends Tile with GeoTiffImageData with GeoTiffSegmentLayoutTransform {
  val cellType: CellType

  val bandCount = 1

  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  private val isTiled = segmentLayout.isTiled

  def getOverviewsCount: Int = overviews.length
  def getOverview(idx: Int): GeoTiffTile = overviews(idx)

  /**
   * Converts the CellType of the GeoTiffTile to the
   * given CellType
   *
   * @param newCellType: The [[CellType]] to be converted to
   * @return A new [[Tile]] that contains the new CellTypes
   */
  def convert(newCellType: CellType): GeoTiffTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)

    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val newBytes = segment.convert(newCellType)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      newCellType,
      overviews = overviews.map(_.convert(newCellType))
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

  def getSegments(ids: Traversable[Int]): Iterator[(Int, GeoTiffSegment)]

  /**
   * Given a col and row, find the segment where this point resides.
   *
   * @param col: The col number
   * @param row: The row number
   * @return An Int that represents the segment's index
   */
  def get(col: Int, row: Int): Int = {
    val segmentIndex = getSegmentIndex(col, row)
    val i = getSegmentTransform(segmentIndex).gridToIndex(col, row)

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
    val segmentIndex = getSegmentIndex(col, row)
    val i = getSegmentTransform(segmentIndex).gridToIndex(col, row)

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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentSize = segment.size

      if(isTiled) {
        // Need to check for bounds
        val segmentTransform = getSegmentTransform(segmentIndex)
        cfor(0)(_ < segmentSize, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i) // TODO: Is there another way to do this?
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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentSize = segment.size

      if(isTiled) {
        // Need to check for bounds
        val segmentTransform = getSegmentTransform(segmentIndex)
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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val newBytes = segment.map(f(_))
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType,
      overviews = overviews
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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val newBytes = segment.mapDouble(f)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType,
      overviews = overviews
    )
  }

  /**
   * Executes an [[IntTileVisitor]] at each cell of the GeoTiffTile.
   *
   * @param visitor: An IntTileVisitor
   */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentSize = segment.size

      val segmentTransform = getSegmentTransform(segmentIndex)

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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentSize = segment.size
      val segmentTransform = getSegmentTransform(segmentIndex)
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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentTransform = getSegmentTransform(segmentIndex)

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
      cellType,
      overviews = overviews
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
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val segmentTransform = getSegmentTransform(segmentIndex)
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
      cellType,
      overviews = overviews
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
        getSegments(0 until segmentCount)
          .zip(otherGeoTiff.getSegments(0 until segmentCount))
          .foreach { case ((segmentIndex, segment), (otherIndex, otherSegment)) =>
            require(segmentIndex == otherIndex, s"Segment index mismatch: $segmentIndex != $otherIndex")
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
          cellType,
          overviews = overviews
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
        getSegments(0 until segmentCount)
          .zip(otherGeoTiff.getSegments(0 until segmentCount))
          .foreach { case ((segmentIndex, segment), (otherIndex, otherSegment)) =>
            require(segmentIndex == otherIndex, s"Segment index mismatch: $segmentIndex != $otherIndex")
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
          cellType,
          overviews = overviews
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
   * Converts GeoTiffTile to a MutableArrayTile
   *
   * @return A [[MutableArrayTile]] of the GeoTiffTile
   */
  def mutable: MutableArrayTile = {
    val tile = ArrayTile.empty(cellType, cols, rows)

    getSegments(0 until segmentCount).foreach { case (segmentId, segment) =>
      val segmentTransform = getSegmentTransform(segmentId)

      if (cellType.isFloatingPoint)
        cfor(0)(_ < segment.size, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if(col < cols && row < rows) {
            val v = segment.getDouble(i)
            tile.setDouble(col, row, v)
          }
        }
      else
        cfor(0)(_ < segment.size, _ + 1) { i =>
          val col = segmentTransform.indexToCol(i)
          val row = segmentTransform.indexToRow(i)
          if(col < cols && row < rows) {
            val v = segment.getInt(i)
            tile.set(col, row, v)
          }
        }
    }

    tile
  }


  /**
   * Crop this tile to given pixel region.
   *
   * @param bounds: Pixel bounds specifying the crop area
   */
  def crop(bounds: GridBounds[Int]): MutableArrayTile = {
    val iter = crop(List(bounds))
    if(iter.isEmpty) throw GeoAttrsError(s"No intersections of ${bounds} vs ${dimensions}")
    else iter.next._2
  }

  /**
    * Crop this tile to given pixel regions.
    *
    * @param windows: Pixel bounds specifying the crop areas
    */
  def crop(windows: Seq[GridBounds[Int]]): Iterator[(GridBounds[Int], MutableArrayTile)] = {
    case class Chip(
      window: GridBounds[Int],
      tile: MutableArrayTile,
      intersectingSegments: Int,
      var segmentsBurned: Int = 0
    )
    val chipsBySegment = scala.collection.mutable.Map.empty[Int, List[Chip]]
    val intersectingSegments = scala.collection.mutable.SortedSet.empty[Int]

    for (window <- windows) {
      val segments: Array[Int] = getIntersectingSegments(window)
      val tile = ArrayTile.empty(cellType, window.width, window.height)
      val chip = Chip(window, tile, segments.length)
      for (segment <- segments) {
        val tail = chipsBySegment.getOrElse(segment, Nil)
        chipsBySegment.update(segment, chip :: tail)
      }
      intersectingSegments ++= segments
    }

    def burnSegments(segmentId: Int, segment: GeoTiffSegment): List[Chip] = {
      var finished: List[Chip] = Nil
      val segmentBounds = getGridBounds(segmentId)
      val segmentTransform = getSegmentTransform(segmentId)

      for (chip <- chipsBySegment(segmentId)) {
        val gridBounds = chip.window
        val overlap = gridBounds.intersection(segmentBounds).get
        if (cellType.isFloatingPoint) {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              val i = segmentTransform.gridToIndex(col, row)
              val v = segment.getDouble(i)
              chip.tile.setDouble(col - gridBounds.colMin, row - gridBounds.rowMin, v)
            }
          }
        } else {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              val i = segmentTransform.gridToIndex(col, row)
              val v = segment.getInt(i)
              chip.tile.set(col - gridBounds.colMin, row - gridBounds.rowMin, v)
            }
          }
        }
        chip.segmentsBurned += 1
        if (chip.segmentsBurned == chip.intersectingSegments)
          finished = chip :: finished
      }

      chipsBySegment.remove(segmentId)
      finished
    }

    getSegments(intersectingSegments).flatMap { case (segmentId, segment) =>
      burnSegments(segmentId, segment)
    }.map { chip =>
      chip.window -> chip.tile
    }
  }

  /**
   * Converts the GeoTiffTile to an Array[Byte]
   *
   * @return An Array[Byte] of the GeoTiffTile
   */
  def toBytes(): Array[Byte] =
    toArrayTile.toBytes
}
