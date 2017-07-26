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
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.split._

import com.typesafe.scalalogging.LazyLogging
import spire.syntax.cfor._

import scala.collection.mutable

object GeoTiffMultibandTile {
  /**
   * Creates a new instances of GeoTiffMultibandTile.
   *
   * @param compressedBytes: An Array[Array[Byte]] that represents the segments in the GeoTiff
   * @param decompressor: A [[Decompressor]] of the GeoTiff
   * @param segmentLayout: The [[GeoTiffSegmentLayout]] of the GeoTiff
   * @param compression: The [[Compression]] type of the data
   * @param bandCount: The number of bands in the GeoTiff
   * @param hasPixelInterleave: Does the GeoTiff have pixel interleave
   * @param cellType: The [[CellType]] of the segments
   * @param bandType: The data storage format of the band. Defaults to None
   * @return An implementation of GeoTiffMultibandTile based off of band or cell type
   */
  def apply(
    segmentBytes: SegmentBytes,
    decompressor: Decompressor,
    segmentLayout: GeoTiffSegmentLayout,
    compression: Compression,
    bandCount: Int,
    hasPixelInterleave: Boolean,
    cellType: CellType,
    bandType: Option[BandType] = None
  ): GeoTiffMultibandTile =
    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: ByteCells =>
            new ByteGeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: UByteCells =>
            new UByteGeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: ShortCells =>
            new Int16GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: UShortCells =>
            new UInt16GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: IntCells =>
            new Int32GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: FloatCells =>
            new Float32GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
          case ct: DoubleCells =>
            new Float64GeoTiffMultibandTile(segmentBytes, decompressor, segmentLayout, compression, bandCount, hasPixelInterleave, ct)
        }
    }

  /** Convert a multiband tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. Only handles pixel interlacing. */
  def apply(tile: MultibandTile): GeoTiffMultibandTile =
    apply(tile, GeoTiffOptions.DEFAULT)

  def apply(tile: MultibandTile, options: GeoTiffOptions): GeoTiffMultibandTile = {
    val bandType = BandType.forCellType(tile.cellType)
    val bandCount = tile.bandCount

    val segmentLayout = GeoTiffSegmentLayout(tile.cols, tile.rows, options.storageMethod, bandType)

    val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
    val compressor = options.compression.createCompressor(segmentCount)

    val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)

    val segmentTiles = Array.ofDim[Array[Tile]](segmentCount)
    cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
      val bandTiles =
        options.storageMethod match {
          case _: Tiled => tile.band(bandIndex).split(segmentLayout.tileLayout)
          case _: Striped => tile.band(bandIndex).split(segmentLayout.tileLayout, Split.Options(extend = false))
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
      val segBytes = Array.ofDim[Byte](cols * rows * bandCount * byteCount)

      val tileBytes = Array.ofDim[Array[Byte]](bandCount)
      cfor(0)(_ < bandCount, _ + 1) { b =>
        tileBytes(b) = tiles(b).toBytes
      }

      var segmentIndex = 0
      cfor(0)(_ < cols * rows, _ + 1) { cellIndex =>
        cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
          cfor(0)(_ < byteCount, _ + 1) { b =>
            val bytes = tileBytes(bandIndex)
            segBytes(segmentIndex) = bytes(cellIndex * byteCount + b)
            segmentIndex += 1
          }
        }
      }

      segmentBytes(i) = compressor.compress(segBytes, i)
    }

    apply(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, bandCount, true, tile.cellType)
  }

}

abstract class GeoTiffMultibandTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  val segmentLayout: GeoTiffSegmentLayout,
  val compression: Compression,
  val bandCount: Int,
  val hasPixelInterleave: Boolean
) extends MultibandTile with GeoTiffImageData with MacroGeotiffMultibandCombiners with LazyLogging {
  val cellType: CellType
  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  def getSegment(i: Int): GeoTiffSegment

  def getSegments(ids: Traversable[Int]): Iterator[(Int, GeoTiffSegment)]

  val segmentCount: Int = segmentBytes.size
  private val isTiled = segmentLayout.isTiled

  /**
   * Returns the corresponding GeoTiffTile from the inputted band index.
   *
   * @param bandIndex: The band's index number
   * @return The corresponding [[GeoTiffTile]]
   */
  def band(bandIndex: Int): GeoTiffTile = {
    require(bandIndex < bandCount,  s"Band $bandIndex does not exist")
    if(hasPixelInterleave) {
      bandType match {
        case BitBandType =>
          val compressedBandBytes = Array.ofDim[Array[Byte]](segmentCount)
          val compressor = compression.createCompressor(segmentCount)

          getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
            val (cols, rows) =
              if (segmentLayout.isTiled) (segmentLayout.tileLayout.tileCols, segmentLayout.tileLayout.tileRows)
              else segmentLayout.getSegmentDimensions(segmentIndex)
            val bytes = GeoTiffSegment.deinterleaveBitSegment(segment, cols, rows, bandCount, bandIndex)
            compressedBandBytes(segmentIndex) = compressor.compress(bytes, segmentIndex)
          }

          GeoTiffTile(new ArraySegmentBytes(compressedBandBytes), compressor.createDecompressor(), segmentLayout, compression, cellType, Some(bandType))
        case _ =>
          val compressedBandBytes = Array.ofDim[Array[Byte]](segmentCount)
          val compressor = compression.createCompressor(segmentCount)
          val bytesPerSample = bandType.bytesPerSample

          getSegments(0 until segmentCount).foreach { case (segmentIndex, geoTiffSegment) =>
            val bytes = GeoTiffSegment.deinterleave(geoTiffSegment.bytes, bandCount, bytesPerSample, bandIndex)
            compressedBandBytes(segmentIndex) = compressor.compress(bytes, segmentIndex)
          }

          GeoTiffTile(new ArraySegmentBytes(compressedBandBytes), compressor.createDecompressor(), segmentLayout, compression, cellType, Some(bandType))
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val compressedBandBytes = Array.ofDim[Array[Byte]](bandSegmentCount)
      val segmentOffset = bandSegmentCount * bandIndex

      segmentBytes.getSegments(segmentOffset until bandSegmentCount + segmentOffset).foreach { case (segmentIndex, segment) =>
        compressedBandBytes(segmentIndex - segmentOffset) = segment.clone
      }

      GeoTiffTile(new ArraySegmentBytes(compressedBandBytes), decompressor, segmentLayout, compression, cellType, Some(bandType))
    }
  }

  /**
    * Converts all of the bands into a collection of Vector[Tile]
    */
  def bands: Vector[Tile] =
    _subsetBands(
      0 until bandCount,
      (segment, cols, rows, bandCount, _) => GeoTiffSegment.deinterleaveBitSegment(segment, cols, rows, bandCount),
      (bytes, bandCount, bytesPerSample, _) => GeoTiffSegment.deinterleave(bytes, bandCount, bytesPerSample)
    ).toVector

  /**
   * Creates an ArrayMultibandTIle that contains a subset of bands
   * from the GeoTiff.
   *
   * @param  bandSequence  A sequence of band indexes that are a subset of bands of the GeoTiff
   * @return               Returns an [[ArrayMultibandTile]] with the selected bands
   */
  def subsetBands(bandSequence: Seq[Int]): ArrayMultibandTile =
    new ArrayMultibandTile(
      _subsetBands(
        bandSequence,
        (segment, cols, rows, bandCount, bandSequence) => GeoTiffSegment.deinterleaveBitSegment(segment, cols, rows, bandCount, bandSequence),
        (bytes, bandCount, bytesPerSample, bandSequence) => GeoTiffSegment.deinterleave(bytes, bandCount, bytesPerSample, bandSequence)
      )
    )

  private def _subsetBands(
    bandSequence: Seq[Int],
    deinterleaveBitSegment: (GeoTiffSegment, Int, Int, Int, Traversable[Int]) => Array[Array[Byte]],
    deinterleave: (Array[Byte], Int, Int, Traversable[Int]) => Array[Array[Byte]]
  ): Array[Tile] = {
    val actualBandCount = bandSequence.size
    val tiles = new Array[Tile](actualBandCount)

    if (hasPixelInterleave) {
      val bands = Array.ofDim[Array[Byte]](bandCount, segmentCount)
      val compressor = compression.createCompressor(segmentCount)
      val decompressor = compressor.createDecompressor()
      bandType match {
        case BitBandType =>
          getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
            val (cols, rows) =
              if (segmentLayout.isTiled) (segmentLayout.tileLayout.tileCols, segmentLayout.tileLayout.tileRows)
              else segmentLayout.getSegmentDimensions(segmentIndex)
            val bytes = deinterleaveBitSegment(segment, cols, rows, bandCount, bandSequence)
            cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
              bands(bandIndex)(segmentIndex) = compressor.compress(bytes(bandIndex), segmentIndex)
            }
          }

          cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
            tiles(bandIndex) =
              GeoTiffTile(
                new ArraySegmentBytes(bands(bandIndex)),
                decompressor,
                segmentLayout,
                compression,
                cellType,
                Some(bandType)
              )
          }
        case _ =>
          val bytesPerSample = bandType.bytesPerSample
          getSegments(0 until segmentCount).foreach { case (segmentIndex, geoTiffSegment) =>
            val bytes = deinterleave(geoTiffSegment.bytes, bandCount, bytesPerSample, bandSequence)
            cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
              bands(bandIndex)(segmentIndex) = compressor.compress(bytes(bandIndex), segmentIndex)
            }
          }

          cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
            tiles(bandIndex) =
              GeoTiffTile(
                new ArraySegmentBytes(bands(bandIndex)),
                decompressor,
                segmentLayout,
                compression,
                cellType,
                Some(bandType)
              )
          }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      val bands = Array.ofDim[Array[Byte]](bandCount, bandSegmentCount)

      cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
        val segmentOffset = bandSegmentCount * bandIndex
        segmentBytes.getSegments(segmentOffset until bandSegmentCount + segmentOffset).foreach { case (segmentIndex, segment) =>
          bands(bandIndex)(segmentIndex - segmentOffset) = segment.clone
        }
      }

      cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
        tiles(bandIndex) =
          GeoTiffTile(
            new ArraySegmentBytes(bands(bandIndex)),
            decompressor,
            segmentLayout,
            compression,
            cellType,
            Some(bandType)
          )
      }
    }

    tiles
  }

  /**
    * Converts the GeoTiffMultibandTile to an
    * [[ArrayMultibandTile]] */
  def toArrayTile(): ArrayMultibandTile =
    ArrayMultibandTile(bands.map(_.toArrayTile): _*)

  /**
   * Performs a crop on itself. The returned MultibandGeoTiffTile will
   * contain bands that have the same area as the inputte GridBounds.
   *
   * @param  gridBounds  A [[GridBounds]] that contains the area to be cropped.
   * @return             A [[ArrayMultibandTile]]
   */
  def crop(gridBounds: GridBounds): ArrayMultibandTile = {
    val bands = Array.fill(bandCount)(ArrayTile.empty(cellType, gridBounds.width, gridBounds.height))
    val intersectingSegments = segmentLayout.intersectingSegments(gridBounds)
    if (hasPixelInterleave) {
      logger.debug(s"Cropping $gridBounds uses ${intersectingSegments.length} out of ${segmentCount} pixel-interleave segments")

      // de-interlace the pixels from each segment
      for ((segmentId, segment) <- getSegments(intersectingSegments)) {
        val segmentBounds = segmentLayout.getGridBounds(segmentId)
        val segmentTransform = segmentLayout.getSegmentTransform(segmentId, bandCount)
        val overlap = gridBounds.intersection(segmentBounds).get

        if (cellType.isFloatingPoint) {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              cfor(0)(_ < bandCount, _ + 1) { band =>
                val i = segmentTransform.gridToIndex(col, row, band)
                val v = segment.getDouble(i)
                bands(band).setDouble(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
            }
          }
        } else {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              cfor(0)(_ < bandCount, _ + 1) { band =>
                val i = segmentTransform.gridToIndex(col, row, band)
                val v = segment.getInt(i)
                bands(band).set(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
            }
          }
        }
      }
    } else {
      logger.debug(s"Cropping $gridBounds uses ${intersectingSegments.length * bandCount} out of ${segmentCount * bandCount} segments")

      val bandSegmentCount = segmentCount / bandCount
      // read segments in a band order
      cfor(0)(_ < bandCount, _ + 1) { band =>
        val segmentOffset = bandSegmentCount * band
        getSegments(intersectingSegments.map(_ + segmentOffset)).foreach { case (id, segment) =>
          val segmentId = id - segmentOffset
          val segmentBounds = segmentLayout.getGridBounds(segmentId)
          val segmentTransform = segmentLayout.getSegmentTransform(segmentId, bandCount)
          val overlap = gridBounds.intersection(segmentBounds).get

          if (cellType.isFloatingPoint) {
            cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
              cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
                val i = segmentTransform.gridToIndex(col, row)
                val v = segment.getDouble(i)
                bands(band).setDouble(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
            }
          } else {
            cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
              cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
                val i = segmentTransform.gridToIndex(col, row)
                val v = segment.getInt(i)
                bands(band).set(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
            }
          }
        }
      }
    }

    ArrayMultibandTile(bands)
  }

  /**
   * Converts the CellTypes of a MultibandTile to the given CellType.
   *
   * @param  newCellType  The desired [[CellType]]
   * @return              A MultibandTile that contains the the new CellType
   */
  def convert(newCellType: CellType): GeoTiffMultibandTile = {
    val arr = Array.ofDim[Array[Byte]](segmentCount)
    val compressor = compression.createCompressor(segmentCount)
    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      val newBytes = segment.convert(newCellType)
      arr(segmentIndex) = compressor.compress(newBytes, segmentIndex)
    }

    GeoTiffMultibandTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      cellType,
      Some(bandType)
    )
  }

  /**
   * Takes a function that takes a GeoTiffSegment and an Int and
   * returns the results as a new MultibandTile.
   *
   * @param  f  A function that takes a [[GeoTiffSegment]] and an Int and returns an Array[Byte]
   * @return    A new MultibandTile that contains the results of the function
   */
  def mapSegments(f: (GeoTiffSegment, Int) => Array[Byte]): MultibandTile = {
    val compressor = compression.createCompressor(segmentCount)
    val arr = Array.ofDim[Array[Byte]](segmentCount)

    getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
      arr(segmentIndex) = compressor.compress(f(segment, segmentIndex), segmentIndex)
    }

    GeoTiffMultibandTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      bandCount,
      hasPixelInterleave,
      cellType,
      Some(bandType)
    )
  }

  /**
    * This function piggy-backs on the other map method to support
    * mapping a subset of the bands.
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile = {
    val set = subset.toSet
    val fn = { (bandIndex: Int, z: Int) =>
      if (set.contains(bandIndex)) f(bandIndex, z)
      else z
    }

    _map(fn)(b => set.contains(b))
  }

  /**
    * This function piggy-backs on the other mapDouble method to
    * support mapping a subset of the bands.
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile = {
    val set = subset.toSet
    val fn = { (bandIndex: Int, z: Double) =>
      if (set.contains(bandIndex)) f(bandIndex, z)
      else z
    }

    _mapDouble(fn)(b => set.contains(b))
  }

  /**
   * Map over a MultibandTile band.
   *
   * @param  b0  The band
   * @param  f   A function that takes an Int and returns an Int
   * @return     Returns a MultibandGeoTiff that contains both the changed and unchanged bands
   */
  def map(b0: Int)(f: Int => Int): MultibandTile =
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

  /**
   * Map over a MultibandTile band.
   *
   * @param  b0  The band
   * @param  f   A function that takes a Double and returns a Double
   * @return     Returns a MultibandGeoTiff that contains both the changed and unchanged bands
   */
  def mapDouble(b0: Int)(f: Double => Double): MultibandTile =
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

  /**
   * Map over a MultibandTile with a function that takes a (Int, Int)
   * and returns an Int.
   *
   * @param  f  A function that takes a (Int, Int) and returns an Int
   * @return    Returns a MultibandGeoTiff that contains the results of f
   */
  def map(f: (Int, Int) => Int): MultibandTile =
    _map(f)(_ => true)
  /** Internal version of mapDouble that potentially iterating
    * over specific bands for band interleave rasters.
    */
  private def _map(f: (Int, Int) => Int)(bandFilter: Int => Boolean): MultibandTile = {
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
        if(bandFilter(bandIndex)) {
        segment.map { z => f(bandIndex, z) }
        } else {
          segment.bytes
        }
      }
    }
  }

  /**
   * Map over a MultibandTile with a function that takes a (Int,
   * Double) and returns a Double.
   *
   * @param  f  A function that takes a (Int, Double) and returns a Double
   * @return    Returns a MultibandGeoTiff that contains the results of f
   */
  def mapDouble(f: (Int, Double) => Double): MultibandTile =
    _mapDouble(f)(_ => true)

  /** Internal version of mapDouble that potentially iterating
    * over specific bands for band interleave rasters.
    */
  private def _mapDouble(f: (Int, Double) => Double)(bandFilter: Int => Boolean): MultibandTile = {
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
        if(bandFilter(bandIndex)) {
          segment.mapDouble { z => f(bandIndex, z) }
        } else {
          segment.bytes
        }
      }
    }
  }

  /**
   * Apply a function that takes an Int and returns Unit over a
   * MultibandTile starting at the given band.
   *
   * @param  b0  The starting band
   * @param  f   A function that takes an Int and returns Unit
   * @return     Returns the Unit value for each Int in the selected bands
   */
  def foreach(b0: Int)(f: Int => Unit): Unit =
    _foreach(b0) { (segment, i) => f(segment.getInt(i)) }

  /**
   * Apply a function that takes a Double and returns Unit over a
   * MultibandTile starting at the given band.
   *
   * @param  b0  The starting band
   * @param  f   A function that takes a Double and returns Unit
   * @return     Returns the Unit value for each Double in the selected bands
   */
  def foreachDouble(b0: Int)(f: Double => Unit): Unit =
    _foreach(b0) { (segment, i) => f(segment.getDouble(i)) }

  private def _foreach(b0: Int)(f: (GeoTiffSegment, Int) => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex, bandCount)
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
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
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
        getSegments(start until start + bandSegmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex % bandSegmentCount, bandCount)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i)
            val row = segmentTransform.indexToRow(i)
            if(col < cols && row < rows) {
              f(segment, i)
            }
          }
        }
      } else {
        getSegments(start until start + bandSegmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(segment, i)
          }
        }
      }
    }
  }

  /**
   * Apply a function that takes a (Int, Int) and returns Unit over a
   * MultibandTile.
   *
   * @param  f  A function that takes a (Int, Int) and returns Unit
   * @return    Returns the Unit value for each (Int, Int) in the MultibandTile
   */
  def foreach(f: (Int, Int) => Unit): Unit =
    _foreach { (segmentIndex, segment, i) =>
      f(segmentIndex, segment.getInt(i))
    }

  /**
   * Apply a function that takes a (Double, Double) and returns Unit
   * over a MultibandTile.
   *
   * @param  f  A function that takes a (Double, Double) and returns Unit
   * @return    Returns the Unit value for each (Double, Double) in the MultibandTile
   */
  def foreachDouble(f: (Int, Double) => Unit): Unit =
    _foreach { (segmentIndex, segment, i) =>
      f(segmentIndex, segment.getDouble(i))
    }

  private def _foreach(f: (Int, GeoTiffSegment, Int) => Unit): Unit = {
    if(hasPixelInterleave) {
      if(isTiled) {
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex, bandCount)
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            val col = segmentTransform.indexToCol(i / bandCount)
            val row = segmentTransform.indexToRow(i / bandCount)
            if(col < cols && row < rows) {
              f(i % bandCount, segment, i)
            }
          }
        }
      } else {
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(i % bandCount, segment, i)
          }
        }
      }
    } else {
      val bandSegmentCount = segmentCount / bandCount
      if(isTiled) {
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val segmentTransform = segmentLayout.getSegmentTransform(segmentIndex, bandCount)
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
        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val bandIndex = segmentIndex / bandSegmentCount

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            f(bandIndex, segment, i)
          }
        }
      }
    }
  }

  def foreach(f: Array[Int] => Unit): Unit = {
    var i = 0
    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val bandValues = Array.ofDim[Int](bandCount)
        cfor(0)(_ < bandCount, _ + 1) { band =>
          bandValues(band) = bands(band).get(col, row)
        }
        f(bandValues)
        i += 1
      }
    }
  }

  def foreachDouble(f: Array[Double] => Unit): Unit = {
    var i = 0
    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val bandValues = Array.ofDim[Double](bandCount)
        cfor(0)(_ < bandCount, _ + 1) { band =>
          bandValues(band) = bands(band).getDouble(col, row)
        }
        f(bandValues)
        i += 1
      }
    }
  }

  /**
    * This function piggy-backs on the other combine method to support
    * combing a subset of the bands.
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })

    val fn = { array: Array[Int] =>
      val data = subset.map({ i => array(i) })
      f(data)
    }

    combine(fn)
  }

  /**
    * This function piggy-backs on the other combineDouble method to
    * support combining a subset of the bands.
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })

    val fn = { array: Array[Double] =>
      val data = subset.map({ i => array(i) })
      f(data)
    }

    combineDouble(fn)
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
      if (hasPixelInterleave) {
        val compressor = compression.createCompressor(segmentCount)
        val arr = Array.ofDim[Array[Byte]](segmentCount)

        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
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

        getSegments(0 until bandSegmentCount).foreach { case (segmentIndex, segment) =>
          val segmentSize = segment.size
          val segmentCombiner = createSegmentCombiner(segmentSize)
          initValueHolder(segmentCombiner)

          cfor(0)(_ < segmentSize, _ + 1) { i =>
            getSegments((0 until bandCount).map(_ * bandSegmentCount + segmentIndex)).foreach { case (index, segment) =>
              val bandIndex = (index - segmentIndex) / bandSegmentCount
              placeValue(segmentCombiner)(segment, i, bandIndex)
            }
            setFromValues(segmentCombiner)(i)
          }

          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        }

        (arr, compressor)
      }

    GeoTiffTile(
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType,
      Some(bandType)
    )
  }

  /**
   * Apply a function that takes a (Int, Int) and returns an Int over
   * two selected bands in the MultibandTile.
   *
   * @param  b0  The first band
   * @param  b1  The second band
   * @param  f   A function that takes a (Int, Int) and returns an Int
   * @return     Returns a new [[Tile]] that contains the results of f
   */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile =
    _combine(b0: Int, b1: Int) { segmentCombiner =>
      { (targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int) =>
        segmentCombiner.set(targetIndex, s1, i1, s2, i2)(f)
      }
    }

  /**
   * Apply a function that takes a (Double, Double) and returns a
   * Double over two selected bands in the MultibandTile.
   *
   * @param  b0  The first band
   * @param  b1  The second band
   * @param  f   A function that takes a (Double, Double) and returns a Double
   * @return     Returns a new [[Tile]] that contains the results of f
   */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile =
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

        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
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
        getSegments(start0 until start0 + bandSegmentCount).zip(getSegments(start1 until start1 + bandSegmentCount)).foreach {
          case ((segmentIndex0, segment0), (_, segment1)) =>
            val segmentIndex = segmentIndex0 - start0
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
      new ArraySegmentBytes(arr),
      compressor.createDecompressor(),
      segmentLayout,
      compression,
      cellType,
      Some(bandType)
    )
  }
}
