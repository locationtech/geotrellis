/*
 * Copyright 2019 Azavea
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

package geotrellis.layer

import geotrellis.raster._
import geotrellis.layer._
import geotrellis.util._

/** Reads tiles by key from a [[RasterSource]] as keyed by a [[LayoutDefinition]]
  * @note It is required that the [[RasterSource]] is pixel aligned with the [[LayoutDefinition]]
  *
  * @param source raster source that can be queried by bounding box
  * @param layout definition of a tile grid over the pixel grid
  * @param tileKeyTransform defines the key transformation you want to apply to the spatially tiled data
  */
class LayoutTileSource[K: SpatialComponent](
  val source: RasterSource,
  val layout: LayoutDefinition,
  val tileKeyTransform: SpatialKey => K
) {
  LayoutTileSource.requireGridAligned(source.gridExtent, layout)

  def sourceColOffset: Long = ((source.extent.xmin - layout.extent.xmin) / layout.cellwidth).toLong
  def sourceRowOffset: Long = ((layout.extent.ymax - source.extent.ymax) / layout.cellheight).toLong

  def rasterRegionForKey(key: K): Option[RasterRegion] = {
    val spatialComponent = key.getComponent[SpatialKey]
    val col = spatialComponent.col.toLong
    val row = spatialComponent.row.toLong
    /**
     * We need to do this manually instead of using RasterExtent.gridBoundsFor because
     * the target pixel area isn't always square.
     */
    val sourcePixelBounds = GridBounds[Long](
      colMin = col * layout.tileCols - sourceColOffset,
      rowMin = row * layout.tileRows - sourceRowOffset,
      colMax = (col+1) * layout.tileCols - 1 - sourceColOffset,
      rowMax = (row+1) * layout.tileRows - 1 - sourceRowOffset
    )

    if (sourcePixelBounds.intersects(source.dimensions))
      Some(RasterRegion(source, sourcePixelBounds))
    else
      None
  }

  def read(key: K): Option[MultibandTile] =
    read(key, 0 until source.bandCount)

  /** Read tile according to key.
    * If tile area intersects source partially the non-intersecting pixels will be filled with NODATA.
    * If tile area does not intersect source None will be returned.
    */
  def read(key: K, bands: Seq[Int]): Option[MultibandTile] = {
    val spatialComponent = key.getComponent[SpatialKey]
    val col = spatialComponent.col.toLong
    val row = spatialComponent.row.toLong
    val sourcePixelBounds = GridBounds(
      colMin = col * layout.tileCols - sourceColOffset,
      rowMin = row * layout.tileRows - sourceRowOffset,
      colMax = (col+1) * layout.tileCols - 1 - sourceColOffset,
      rowMax = (row+1) * layout.tileRows - 1 - sourceRowOffset
    )

    for {
      bounds <- sourcePixelBounds.intersection(source.dimensions)
      raster <- source.read(bounds, bands)
    } yield {
      if (raster.tile.cols == layout.tileCols && raster.tile.rows == layout.tileRows) {
        raster.tile
      } else {
        // raster is smaller but not bigger than I think ...
        // its offset is relative to the raster we wished we had
        val colOffset = bounds.colMin - sourcePixelBounds.colMin
        val rowOffset = bounds.rowMin - sourcePixelBounds.rowMin
        raster.tile.mapBands { (_, band) =>
          PaddedTile(band, colOffset.toInt, rowOffset.toInt, layout.tileCols, layout.tileRows)
        }
      }
    }
  }

  /** Read multiple tiles according to key.
    * If each tile area intersects source partially the non-intersecting pixels will be filled with NODATA.
    * If tile area does not intersect source it will be excluded from result iterator.
    */
  def readAll(keys: Iterator[K], bands: Seq[Int]): Iterator[(K, MultibandTile)] =
    for {
      key <- keys
      spatialComponent = key.getComponent[SpatialKey]
      col = spatialComponent.col.toLong
      row = spatialComponent.row.toLong
      sourcePixelBounds = GridBounds(
        colMin = col * layout.tileCols - sourceColOffset,
        rowMin = row * layout.tileRows - sourceRowOffset,
        colMax = (col+1) * layout.tileCols - 1 - sourceColOffset,
        rowMax = (row+1) * layout.tileRows - 1 - sourceRowOffset
      )
      bounds <- sourcePixelBounds.intersection(source.dimensions)
      raster <- source.read(bounds, bands)
    } yield {
      val tile =
        if (raster.tile.cols == layout.tileCols && raster.tile.rows == layout.tileRows) {
          raster.tile
        } else {
          // raster is smaller but not bigger than I think ...
          // its offset is relative to the raster we wished we had
          val colOffset = bounds.colMin - sourcePixelBounds.colMin
          val rowOffset = bounds.rowMin - sourcePixelBounds.rowMin
          raster.tile.mapBands { (_, band) =>
            PaddedTile(band, colOffset.toInt, rowOffset.toInt, layout.tileCols, layout.tileRows)
          }
        }
      (key, tile)
    }

  def readAll(keys: Iterator[K]): Iterator[(K, MultibandTile)] =
    readAll(keys, 0 until source.bandCount)

  /** Read all available tiles */
  def readAll(): Iterator[(K, MultibandTile)] =
    readAll(keys.toIterator)

  /** Set of keys that can be read from this tile source */
  def keys: Set[K] = {
    lazy val buffX = layout.cellSize.width * -0.25
    lazy val buffY = layout.cellSize.height * -0.25

    layout.extent.intersection(source.extent) match {
      case Some(intersection) =>
        /**
         * Buffered down by a quarter of a pixel size in order to
         * avoid floating point errors that can occur during
         * key generation.
         */
        val buffered = intersection.copy(
          intersection.xmin - buffX,
          intersection.ymin - buffY,
          intersection.xmax + buffX,
          intersection.ymax + buffY
        )

        layout.mapTransform.keysForGeometry(buffered.toPolygon).map(tileKeyTransform)
      case None =>
        Set.empty[K]
    }
  }

  /** All intersecting RasterRegions with their respective keys */
  def keyedRasterRegions(): Iterator[(K, RasterRegion)] =
    keys
      .toIterator
      .flatMap { key =>
          val result = rasterRegionForKey(key)
          result.map { region => (key, region) }
      }
}

object LayoutTileSource {
  def apply[K: SpatialComponent](source: RasterSource, layout: LayoutDefinition, tileKeyTransform: SpatialKey => K): LayoutTileSource[K] =
    new LayoutTileSource(source, layout, tileKeyTransform)

  def spatial(source: RasterSource, layout: LayoutDefinition): LayoutTileSource[SpatialKey] =
    new LayoutTileSource(source, layout, identity)

  def temporal(source: RasterSource, layout: LayoutDefinition, tileKeyTransform: SpatialKey => SpaceTimeKey): LayoutTileSource[SpaceTimeKey] =
    new LayoutTileSource(source, layout, tileKeyTransform)

  private def requireGridAligned(a: GridExtent[Long], b: GridExtent[Long]): Unit = {
    import org.scalactic._
    import TripleEquals._
    import Tolerance._

    val epsX: Double = math.min(a.cellwidth, b.cellwidth) * 0.01
    val epsY: Double = math.min(a.cellheight, b.cellheight) * 0.01

    require((a.cellwidth === b.cellwidth +- epsX) && (a.cellheight === b.cellheight +- epsY),
      s"CellSize differs: ${a.cellSize}, ${b.cellSize}")

    @inline def offset(a: Double, b: Double, w: Double): Double = {
      val cols = (a - b) / w
      cols - math.floor(cols)
    }


    val deltaX = math.round((a.extent.xmin - b.extent.xmin) / b.cellwidth)
    val deltaY = math.round((a.extent.ymin - b.extent.ymin) / b.cellheight)

    /**
     * resultX and resultY represent the pixel bounds of b that is
     * closest to the a.extent.xmin and a.extent.ymin.
     */

    val resultX = deltaX * b.cellwidth + b.extent.xmin
    val resultY = deltaY * b.cellheight + b.extent.ymin

    /**
      * TODO: This is ignored at the moment to make it soft and to make GDAL work,
      * we need to reconsider these things to be softer (?)
      */

    require(a.extent.xmin === resultX +- epsX,
      s"x-aligned: offset by ${a.cellSize} ${offset(a.extent.xmin, resultX, a.cellwidth)}")

    require(a.extent.ymin === resultY +- epsY,
      s"y-aligned: offset by ${a.cellSize} ${offset(a.extent.ymin, resultY, a.cellheight)}")
  }
}
