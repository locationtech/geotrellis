/*
 * Copyright 2018 Azavea
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

import geotrellis.raster.io.geotiff.compression.Compression
import geotrellis.raster._
import geotrellis.proj4.CRS
import geotrellis.vector.Extent
import geotrellis.util._
import spire.syntax.cfor._


trait GeoTiffBuilder[T <: CellGrid] extends Serializable {
  /** Make GeoTiff Tile from component segments.
    * Missing segments will be substituted with NODATA.
    * Segments must be keyed relative to (0, 0) offset.
    * Produced GeoTiff will include every pixel of the segment tiles.
    *
    * @param segments keyed by (column, row) in tile layout
    * @param tileLayout of the segments
    * @param cellType of desired tile
    * @param storageMethod for multiband tiles
    * @param compression method for segments
    */
  def makeTile(
    segments: Iterator[(Product2[Int, Int], T)],
    tileLayout: TileLayout,
    cellType: CellType,
    storageMethod: StorageMethod,
    compression: Compression
  ): T = {
    val segmentLayout = GeoTiffSegmentLayout(
      totalCols = tileLayout.totalCols.toInt,
      totalRows = tileLayout.totalRows.toInt,
      storageMethod,
      PixelInterleave,
      BandType.forCellType(cellType))

    makeTile(segments, segmentLayout, cellType, compression)
  }

  /** Make GeoTiff Tile from component segments.
    * Missing segments will be substituted with NODATA.
    * Segments must be keyed relative to (0, 0) offset.
    * Note that [[GeoTiffSegmentLayout]] may specify pixel range
    *  smaller than covered by all the tiles, introducing a buffer.
    *
    * @param segments keyed by (column, row) in tile layout
    * @param segmentLayout of the GeoTiff segments
    * @param cellType of desired tile
    * @param compression method for segments
    */
  def makeTile(
    segments: Iterator[(Product2[Int, Int], T)],
    segmentLayout: GeoTiffSegmentLayout,
    cellType: CellType,
    compression: Compression
  ): T

  /** Abstracts over GeoTiff class constructor */
  def makeGeoTiff(
    tile: T,
    extent: Extent,
    crs: CRS,
    tags: Tags,
    options: GeoTiffOptions
  ): GeoTiff[T]


  def fromSegments(
    segments: Map[_ <: Product2[Int, Int], T],
    tileExtent: (Int, Int) => Extent,
    crs: CRS,
    options: GeoTiffOptions,
    tags: Tags = Tags.empty
  ): GeoTiff[T] = {
    val sample = segments.head._2
    val cellType = sample.cellType
    val tileCols = sample.cols
    val tileRows = sample.rows

    val keys = segments.keys.toList.sortBy { p => (p._1, p._2) }
    val firstKey = keys.head
    val lastKey = keys.last
    val colMin: Int = firstKey._1
    val rowMin: Int = firstKey._2
    val colMax: Int = lastKey._1
    val rowMax: Int = lastKey._2

    val opts = options.copy(storageMethod = Tiled(tileCols, tileRows))

    val tile: T = makeTile(
      segments.iterator.map { case (key, tile) =>
        ((key._1 - colMin , key._2 - rowMin), tile)
      },
      TileLayout(colMax - colMin + 1, rowMax - rowMin + 1, tileCols, tileRows),
      segments.head._2.cellType,
      options.storageMethod,
      options.compression
    )

    val extent = tileExtent(colMin, rowMin) combine tileExtent(colMax, rowMax)

    makeGeoTiff(tile, extent, crs, tags, opts)
  }
}

object GeoTiffBuilder {
  def apply[T <: CellGrid: GeoTiffBuilder] = implicitly[GeoTiffBuilder[T]]

  implicit val singlebandGeoTiffBuilder = new GeoTiffBuilder[Tile] {
    def makeTile(
      segments: Iterator[(Product2[Int, Int], Tile)],
      segmentLayout: GeoTiffSegmentLayout,
      cellType: CellType,
      compression: Compression
    ) = {
      val tileLayout = segmentLayout.tileLayout
      val segmentCount = tileLayout.layoutCols * tileLayout.layoutRows
      val compressor = compression.createCompressor(segmentCount)

      val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)

      segments.foreach { case (key, tile) =>
        val layoutCol = key._1
        val layoutRow = key._2
        require(layoutCol < tileLayout.layoutCols, s"col $layoutCol < ${tileLayout.layoutCols}")
        require(layoutRow < tileLayout.layoutRows, s"row $layoutRow < ${tileLayout.layoutRows}")
        val index = tileLayout.layoutCols * layoutRow + layoutCol
        val bytes = tile.interpretAs(cellType).toBytes
        segmentBytes(index) = compressor.compress(bytes, index)
      }

      lazy val emptySegment =
        ArrayTile.empty(cellType, tileLayout.tileCols, tileLayout.tileRows).toBytes

      cfor (0)(_ < segmentBytes.length, _ + 1){ index =>
        if (null == segmentBytes(index)) {
          segmentBytes(index) = compressor.compress(emptySegment, index)
        }
      }

      GeoTiffTile(
        new ArraySegmentBytes(segmentBytes),
        compressor.createDecompressor,
        segmentLayout,
        compression,
        cellType)
    }

    def makeGeoTiff(
      tile: Tile,
      extent: Extent,
      crs: CRS,
      tags: Tags,
      options: GeoTiffOptions
    ) = SinglebandGeoTiff(tile, extent, crs, tags, options)
  }

  implicit val multibandGeoTiffBuilder = new GeoTiffBuilder[MultibandTile] {
    def makeTile(
      segments: Iterator[(Product2[Int, Int], MultibandTile)],
      segmentLayout: GeoTiffSegmentLayout,
      cellType: CellType,
      compression: Compression
    ) = {
      val buffered = segments.buffered
      val bandCount = buffered.head._2.bandCount
      val tileLayout = segmentLayout.tileLayout
      val cols = tileLayout.tileCols
      val rows = tileLayout.tileRows

      val (segmentBytes, compressor) = segmentLayout.interleaveMethod match {
        case PixelInterleave =>
          val segmentCount = tileLayout.layoutCols * tileLayout.layoutRows
          val compressor = compression.createCompressor(segmentCount)
          val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)

          buffered.foreach { case (key, tile) =>
            val layoutCol = key._1
            val layoutRow = key._2
            val index = tileLayout.layoutCols * layoutRow + layoutCol
            val bytes = GeoTiffSegment.pixelInterleave(tile.interpretAs(cellType))
            segmentBytes(index) = compressor.compress(bytes, index)
          }

          lazy val emptySegment =
            GeoTiffSegment.pixelInterleave(
              MultibandTile(
                Array.fill(bandCount)(ArrayTile.empty(cellType, cols, rows))))

          cfor(0)(_ < segmentBytes.length, _ + 1) { index =>
            if (null == segmentBytes(index))
              segmentBytes(index) = compressor.compress(emptySegment, index)
          }

          (segmentBytes, compressor)

        case BandInterleave =>
          val bandSegmentCount = tileLayout.layoutCols * tileLayout.layoutRows
          val segmentCount = bandSegmentCount * bandCount
          val compressor = compression.createCompressor(segmentCount)
          val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)

          buffered.foreach { case (key, tile)  =>
            cfor(0)( _ < tile.bandCount, _ + 1) { bandIndex =>
              val layoutCol = key._1
              val layoutRow = key._2
              val bandSegmentOffset = bandSegmentCount * bandIndex
              val index = tileLayout.layoutCols * layoutRow + layoutCol + bandSegmentOffset
              val bytes = tile.band(bandIndex).interpretAs(cellType).toBytes
              segmentBytes(index) = compressor.compress(bytes, index)
            }
          }

          (segmentBytes, compressor)
      }

      GeoTiffMultibandTile(
        new ArraySegmentBytes(segmentBytes),
        compressor.createDecompressor,
        segmentLayout,
        compression,
        bandCount,
        cellType)
    }

    def makeGeoTiff(
      tile: MultibandTile,
      extent: Extent,
      crs: CRS,
      tags: Tags,
      options: GeoTiffOptions
    ) = MultibandGeoTiff(tile, extent, crs, tags, options)
  }
}
