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
import spire.syntax.cfor._
import scala.collection.mutable

object GeoTiffMultibandTile {
  /**
   * Creates a new instances of GeoTiffMultibandTile.
   *
   * @param segmentBytes: An Array[Array[Byte]] that represents the segments in the GeoTiff
   * @param decompressor: A [[Decompressor]] of the GeoTiff
   * @param segmentLayout: The [[GeoTiffSegmentLayout]] of the GeoTiff
   * @param compression: The [[Compression]] type of the data
   * @param bandCount: The number of bands in the GeoTiff
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
    cellType: CellType,
    bandType: Option[BandType] = None,
    overviews: List[GeoTiffMultibandTile] = Nil
  ): GeoTiffMultibandTile =
    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt32GeoTiffMultibandTile => gt }
            )
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: BitGeoTiffMultibandTile => gt }
            )
          case ct: ByteCells =>
            new ByteGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: ByteGeoTiffMultibandTile => gt }
            )
          case ct: UByteCells =>
            new UByteGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UByteGeoTiffMultibandTile => gt }
            )
          case ct: ShortCells =>
            new Int16GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int16GeoTiffMultibandTile => gt }
            )
          case ct: UShortCells =>
            new UInt16GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt16GeoTiffMultibandTile => gt }
            )
          case ct: IntCells =>
            new Int32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int32GeoTiffMultibandTile => gt }
            )
          case ct: FloatCells =>
            new Float32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float32GeoTiffMultibandTile => gt }
            )
          case ct: DoubleCells =>
            new Float64GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float64GeoTiffMultibandTile => gt }
            )
        }
    }

  def applyOverview(
    geoTiffTile: GeoTiffMultibandTile,
    compression: Compression,
    cellType: CellType,
    bandType: Option[BandType] = None
  ): GeoTiffMultibandTile = {
    val segmentCount = geoTiffTile.segmentCount
    val segmentBytes = geoTiffTile.segmentBytes
    val segmentLayout = geoTiffTile.segmentLayout
    val compressor = compression.createCompressor(segmentCount)
    val decompressor = compressor.createDecompressor()
    val bandCount = geoTiffTile.bandCount
    val overviews = geoTiffTile.overviews.map(applyOverview(_, compression, cellType, bandType))

    bandType match {
      case Some(UInt32BandType) =>
        cellType match {
          case ct: FloatCells =>
            new UInt32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt32GeoTiffMultibandTile => gt }
            )
          case _ =>
            throw new IllegalArgumentException("UInt32BandType should always resolve to Float celltype")
        }
      case _ =>
        cellType match {
          case ct: BitCells =>
            new BitGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: BitGeoTiffMultibandTile => gt }
            )
          case ct: ByteCells =>
            new ByteGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: ByteGeoTiffMultibandTile => gt }
            )
          case ct: UByteCells =>
            new UByteGeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UByteGeoTiffMultibandTile => gt }
            )
          case ct: ShortCells =>
            new Int16GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int16GeoTiffMultibandTile => gt }
            )
          case ct: UShortCells =>
            new UInt16GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: UInt16GeoTiffMultibandTile => gt }
            )
          case ct: IntCells =>
            new Int32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Int32GeoTiffMultibandTile => gt }
            )
          case ct: FloatCells =>
            new Float32GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float32GeoTiffMultibandTile => gt }
            )
          case ct: DoubleCells =>
            new Float64GeoTiffMultibandTile(
              segmentBytes,
              decompressor,
              segmentLayout,
              compression,
              bandCount,
              ct,
              overviews.map(applyOverview(_, compression, cellType, bandType)).collect { case gt: Float64GeoTiffMultibandTile => gt }
            )
        }
    }
  }

  /** Convert a multiband tile to a GeoTiffTile. Defaults to Striped GeoTIFF format. Only handles pixel interlacing. */
  def apply(tile: MultibandTile): GeoTiffMultibandTile =
    apply(tile, GeoTiffOptions.DEFAULT)

  def apply(tile: MultibandTile, options: GeoTiffOptions): GeoTiffMultibandTile = {
    val bandType = BandType.forCellType(tile.cellType)
    val segmentLayout = GeoTiffSegmentLayout(
      tile.cols, tile.rows, options.storageMethod, options.interleaveMethod, bandType)

    val segmentPixelCols = segmentLayout.tileLayout.tileCols
    val segmentPixelRows = segmentLayout.tileLayout.tileRows

    val segments: Iterator[((Int, Int), MultibandTile)] =
      for {
        windowRowMin <- Iterator.range(start = 0, end = tile.rows, step = segmentPixelRows)
        windowColMin <- Iterator.range(start = 0, end = tile.cols, step = segmentPixelCols)
      } yield {
        val bounds = GridBounds(
          colMin = windowColMin,
          rowMin = windowRowMin,
          colMax = windowColMin + segmentPixelCols - 1,
          rowMax = windowRowMin + segmentPixelRows - 1)

        val key = (bounds.colMin / segmentPixelCols, bounds.rowMin / segmentPixelRows)
        val bands: Seq[Tile] =
          for (bandIndex <- 0 until tile.bandCount)
          yield CroppedTile(tile.band(bandIndex), bounds)

        (key, ArrayMultibandTile(bands.toArray))
      }

    GeoTiffBuilder[MultibandTile]
      .makeTile(segments, segmentLayout, tile.cellType, options.compression)
      .asInstanceOf[GeoTiffMultibandTile] // This is always safe in current implementation
  }
}

abstract class GeoTiffMultibandTile(
  val segmentBytes: SegmentBytes,
  val decompressor: Decompressor,
  val segmentLayout: GeoTiffSegmentLayout,
  val compression: Compression,
  val bandCount: Int,
  val overviews: List[GeoTiffMultibandTile] = Nil
) extends MultibandTile with GeoTiffImageData with GeoTiffSegmentLayoutTransform with MacroGeotiffMultibandCombiners {
  val cellType: CellType
  val cols: Int = segmentLayout.totalCols
  val rows: Int = segmentLayout.totalRows

  def hasPixelInterleave = segmentLayout.hasPixelInterleave

  def getSegment(i: Int): GeoTiffSegment

  def getSegments(ids: Traversable[Int]): Iterator[(Int, GeoTiffSegment)]

  val segmentCount: Int = segmentBytes.size
  private val isTiled = segmentLayout.isTiled

  def getOverviewsCount: Int = overviews.length
  def getOverview(idx: Int): GeoTiffMultibandTile = overviews(idx)

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
            val dims =
              if (segmentLayout.isTiled)
                Dimensions(segmentLayout.tileLayout.tileCols, segmentLayout.tileLayout.tileRows)
              else
                getSegmentDimensions(segmentIndex)
            val bytes = GeoTiffSegment.deinterleaveBitSegment(segment, dims, bandCount, bandIndex)
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
      (segment, dims, bandCount, _) => GeoTiffSegment.deinterleaveBitSegment(segment, dims, bandCount),
      (bytes, bandCount, bytesPerSample, _) => GeoTiffSegment.deinterleave(bytes, bandCount, bytesPerSample)
    ).toVector

  // TODO: do this smartly

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
        (segment, dims, bandCount, bandSequence) => GeoTiffSegment.deinterleaveBitSegment(segment, dims, bandCount, bandSequence),
        (bytes, bandCount, bytesPerSample, bandSequence) => GeoTiffSegment.deinterleave(bytes, bandCount, bytesPerSample, bandSequence)
      )
    )

  private def _subsetBands(
    bandSequence: Seq[Int],
    deinterleaveBitSegment: (GeoTiffSegment, Dimensions[Int], Int, Traversable[Int]) => Array[Array[Byte]],
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
            val dims: Dimensions[Int] =
              if (segmentLayout.isTiled)
                Dimensions(segmentLayout.tileLayout.tileCols, segmentLayout.tileLayout.tileRows)
              else
                getSegmentDimensions(segmentIndex)
            val bytes = deinterleaveBitSegment(segment, dims, bandCount, bandSequence)
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
    crop(GridBounds(this.dimensions))

  /**
   * Crop this tile to given pixel region.
   *
   * @param bounds Pixel bounds specifying the crop area.
   */
 def crop(bounds: GridBounds[Int]): ArrayMultibandTile =
   crop(bounds, (0 until bandCount).toArray)

  /**
   * Crop this tile to given pixel region of the given bands. The returned MultibandGeoTiffTile
   * will contain a subset of bands that have the same area as the input GridBounds.
   *
   * @param bounds Pixel bounds specifying the crop area.
   * @param  bandIndices       An array of band indexes.
   *
   */
 def crop(bounds: GridBounds[Int], bandIndices: Array[Int]): ArrayMultibandTile = {
   val iter = crop(List(bounds), bandIndices)
   if (iter.isEmpty) throw GeoAttrsError(s"No intersections of ${bounds} vs ${dimensions}")
   else iter.next._2
 }

  /**
    * Performs a crop  operaiton.
    *
    * @param  windows  Pixel bounds specifying the crop areas
    */
  def crop(windows: Seq[GridBounds[Int]]): Iterator[(GridBounds[Int], ArrayMultibandTile)] =
    crop(windows, (0 until bandCount).toArray)

  /**
    * Performs a crop and band subsetting operaiton. The returned MultibandGeoTiffTile will
    * contain a subset of bands that have the same area as the input GridBounds.
    * The bands will be in the order given.
    *
    * @param  windows  Pixel bounds specifying the crop areas
    * @param  bandIndices       An array of band indexes.
    *
    */
  def crop(windows: Seq[GridBounds[Int]], bandIndices: Array[Int]): Iterator[(GridBounds[Int], ArrayMultibandTile)] = {
    val bandSubsetLength = bandIndices.length

    case class Chip(
      window: GridBounds[Int],
      bands: Array[MutableArrayTile],
      intersectingSegments: Int,
      var segmentsBurned: Int = 0
    )
    val chipsBySegment = mutable.Map.empty[Int, List[Chip]]
    val intersectingSegments = mutable.SortedSet.empty[(Int, Int)]

    for (window <- windows) {
      val segments: Array[(Int, Int)] =
        if (hasPixelInterleave)
          getIntersectingSegments(window).map(i => 0 -> i)
        else
          getIntersectingSegments(window, bandIndices)

      val bands = Array.fill(bandSubsetLength)(ArrayTile.empty(cellType, window.width, window.height))
      val chip = Chip(window, bands, segments.length)
      for (segment <- segments.map(_._2)) {
        val tail = chipsBySegment.getOrElse(segment, Nil)
        chipsBySegment.update(segment, chip :: tail)
      }
      intersectingSegments ++= segments
    }

    def burnPixelInterleave(segmentId: Int, segment: GeoTiffSegment): List[Chip] = {
      var finished: List[Chip] = Nil
      val segmentBounds = getGridBounds(segmentId)
      val segmentTransform = getSegmentTransform(segmentId)

      for (chip <- chipsBySegment(segmentId)) {
        val gridBounds = chip.window
        val overlap = gridBounds.intersection(segmentBounds).get
        if (cellType.isFloatingPoint) {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              cfor(0)(_ < bandSubsetLength, _ + 1) { bandIndex =>
                val i = segmentTransform.gridToIndex(col, row, bandIndices(bandIndex))
                val v = segment.getDouble(i)
                chip.bands(bandIndex).setDouble(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
            }
          }
        } else {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              cfor(0)(_ < bandSubsetLength, _ + 1) { bandIndex =>
                val i = segmentTransform.gridToIndex(col, row, bandIndices(bandIndex))
                val v = segment.getInt(i)
                chip.bands(bandIndex).set(col - gridBounds.colMin, row - gridBounds.rowMin, v)
              }
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


    def burnBandInterleave(segmentId: Int, subsetBandIndex: Int, segment: GeoTiffSegment): List[Chip] = {
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
              chip.bands(subsetBandIndex).setDouble(col - gridBounds.colMin, row - gridBounds.rowMin, v)
            }
          }
        } else {
          cfor(overlap.colMin)(_ <= overlap.colMax, _ + 1) { col =>
            cfor(overlap.rowMin)(_ <= overlap.rowMax, _ + 1) { row =>
              val i = segmentTransform.gridToIndex(col, row)
              val v = segment.getInt(i)
              chip.bands(subsetBandIndex).set(col - gridBounds.colMin, row - gridBounds.rowMin, v)
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

    val chips =
      if (hasPixelInterleave) {
        getSegments(intersectingSegments.map(_._2)).flatMap { case (segmentId, segment) =>
          burnPixelInterleave(segmentId, segment)
        }
      } else {
        val bandSegmentCount = segmentCount / bandCount
        val segmentBandMap = intersectingSegments.map { case (f, s) => (s, f) }.toMap
        val bandIndexToSubsetIndex = bandIndices.zipWithIndex.toMap

        getSegments(intersectingSegments.map(_._2)).flatMap { case (segmentId, segment) =>
          val bandIndex = segmentBandMap(segmentId)
          val subsetBandIndex = bandIndexToSubsetIndex(bandIndex)

          burnBandInterleave(segmentId, subsetBandIndex, segment)
        }
      }

    chips.map { chip =>
      chip.window -> ArrayMultibandTile(chip.bands)
    }
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
      newCellType,
      Some(bandType),
      overviews = overviews.map(_.convert(newCellType))
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
      cellType,
      Some(bandType),
      overviews = overviews
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
          val segmentTransform = getSegmentTransform(segmentIndex)
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
          val segmentTransform = getSegmentTransform(segmentIndex % bandSegmentCount)
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
          val segmentTransform = getSegmentTransform(segmentIndex)
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
          val segmentTransform = getSegmentTransform(segmentIndex)
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
