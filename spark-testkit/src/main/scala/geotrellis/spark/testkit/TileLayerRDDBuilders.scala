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

package geotrellis.spark.testkit

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.ingest._
import org.apache.spark._
import org.apache.spark.rdd.RDD
import jp.ne.opt.chronoscala.Imports._
import java.time.{ZoneOffset, ZonedDateTime}

import geotrellis.layers.TileLayerMetadata

import scala.collection.mutable

object TileLayerRDDBuilders extends TileLayerRDDBuilders

trait TileLayerRDDBuilders {

  lazy val defaultCRS = LatLng

  /** Cuts the raster according to the layoutCols and layoutRows given.
    * Returns the raster that was used to fit inside the tile layout, which might
    * be smaller than the input raster
    *
    * Metadata mapTransform is accurate.
    */
  def createTileLayerRDD(
    input: Raster[Tile],
    layoutCols: Int,
    layoutRows: Int
  )(implicit sc: SparkContext): (Raster[Tile], TileLayerRDD[SpatialKey]) =
    createTileLayerRDD(input, layoutCols, layoutRows, defaultCRS)

  /** Cuts the raster according to the layoutCols and layoutRows given.
    * Returns the raser that was used to fit inside the tile layout, which might
    * be smaller than the input raster.
    *
    * Metadata mapTransform is accurate.
    */
  def createTileLayerRDD(
    raster: Raster[Tile],
    layoutCols: Int,
    layoutRows: Int,
    crs: CRS
  )(implicit sc: SparkContext): (Raster[Tile], TileLayerRDD[SpatialKey]) = {
    val Raster(input, extent) = raster
    val (cols, rows) = (input.cols, input.rows)

    val tileLayout =
      if (layoutCols >= cols || layoutRows >= rows)
        sys.error(s"Invalid for tile of dimensions ${(cols, rows)}: ${(layoutCols, layoutRows)}")
      else
        TileLayout(layoutCols, layoutRows, cols / layoutCols, rows / layoutRows)

    val tile: Tile =
      if(tileLayout.totalCols.toInt != cols || tileLayout.totalRows.toInt != rows) {
        input.crop(tileLayout.totalCols.toInt, tileLayout.totalRows.toInt)
      } else
        input

    val e = raster.rasterExtent.extentFor(GridBounds(0, 0, tile.cols - 1, tile.rows - 1))

    val resultRaster = Raster(tile, e)
    (resultRaster, createTileLayerRDD(sc, resultRaster, tileLayout, crs))
  }

  /** Cuts the tile according to the layoutCols and layoutRows given.
    * Returns the tile that was used to fit inside the tile layout, which might
    * be smaller than the input tile
    */
  def createTileLayerRDD(
    input: Tile,
    layoutCols: Int,
    layoutRows: Int
  )(implicit sc: SparkContext): (Tile, TileLayerRDD[SpatialKey]) =
    createTileLayerRDD(input, layoutCols, layoutRows, defaultCRS)

  /** Cuts the tile according to the layoutCols and layoutRows given.
    * Returns the tile that was used to fit inside the tile layout, which might
    * be smaller than the input tile
    */
  def createTileLayerRDD(
    input: Tile,
    layoutCols: Int,
    layoutRows: Int,
    crs: CRS
  )(implicit sc: SparkContext): (Tile, TileLayerRDD[SpatialKey]) = {
    val (cols, rows) = (input.cols, input.rows)

    val tileLayout =
      if (layoutCols >= cols || layoutRows >= rows)
        sys.error(s"Invalid for tile of dimensions ${(cols, rows)}: ${(layoutCols, layoutRows)}")
      else
        TileLayout(layoutCols, layoutRows, cols / layoutCols, rows / layoutRows)

    val tile: Tile =
      if(tileLayout.totalCols.toInt != cols || tileLayout.totalRows.toInt != rows) {
        input.crop(tileLayout.totalCols.toInt, tileLayout.totalRows.toInt)
      } else
        input

    (tile, createTileLayerRDD(sc, tile, tileLayout, crs))
  }

  def createTileLayerRDD(
    tile: Tile,
    tileLayout: TileLayout
  )(implicit sc: SparkContext): TileLayerRDD[SpatialKey] =
    createTileLayerRDD(sc, tile, tileLayout)

  def createTileLayerRDD(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout
  ): TileLayerRDD[SpatialKey] =
    createTileLayerRDD(sc, tile, tileLayout, defaultCRS)

  def createTileLayerRDD(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout,
    crs: CRS
  ): TileLayerRDD[SpatialKey] = {
    val extent = crs.worldExtent
    createTileLayerRDD(sc, Raster(tile, extent), tileLayout, crs)
  }

  def createTileLayerRDD(
    sc: SparkContext,
    raster: Raster[Tile],
    tileLayout: TileLayout
  ): TileLayerRDD[SpatialKey] =
    createTileLayerRDD(sc, raster, tileLayout, defaultCRS)

  def createTileLayerRDD(
    sc: SparkContext,
    raster: Raster[Tile],
    tileLayout: TileLayout,
    crs: CRS
  ): TileLayerRDD[SpatialKey] = {
    val layoutScheme = FloatingLayoutScheme(tileLayout.tileCols, tileLayout.tileRows)
    val inputRdd = sc.parallelize(Seq((ProjectedExtent(raster.extent, crs), raster.tile)))

    val (_, metadata) = inputRdd.collectMetadata[SpatialKey](crs, layoutScheme)

    val tiled: RDD[(SpatialKey, Tile)] = inputRdd.cutTiles(metadata)

    new ContextRDD(tiled, metadata)
  }

  def createMultibandTileLayerRDD(
    tile: MultibandTile,
    tileLayout: TileLayout
  )(implicit sc: SparkContext): MultibandTileLayerRDD[SpatialKey] =
    createMultibandTileLayerRDD(sc, tile, tileLayout)

  def createMultibandTileLayerRDD(
    sc: SparkContext,
    tile: MultibandTile,
    tileLayout: TileLayout
  ): MultibandTileLayerRDD[SpatialKey] =
    createMultibandTileLayerRDD(sc, tile, tileLayout, defaultCRS)

  def createMultibandTileLayerRDD(
    sc: SparkContext,
    tile: MultibandTile,
    tileLayout: TileLayout,
    crs: CRS
  ): MultibandTileLayerRDD[SpatialKey] = {
    val extent = crs.worldExtent
    createMultibandTileLayerRDD(sc, Raster(tile, extent), tileLayout, crs)
  }

  def createMultibandTileLayerRDD(
    sc: SparkContext,
    raster: Raster[MultibandTile],
    tileLayout: TileLayout
  ): MultibandTileLayerRDD[SpatialKey] =
    createMultibandTileLayerRDD(sc, raster, tileLayout, defaultCRS)

  def createMultibandTileLayerRDD(
    sc: SparkContext,
    raster: Raster[MultibandTile],
    tileLayout: TileLayout,
    crs: CRS
  ): MultibandTileLayerRDD[SpatialKey] = {
    val layoutScheme = FloatingLayoutScheme(tileLayout.tileCols, tileLayout.tileRows)
    val inputRdd = sc.parallelize(Seq((ProjectedExtent(raster.extent, crs), raster.tile)))

    val (_, metadata) = inputRdd.collectMetadata[SpatialKey](crs, layoutScheme)

    val tiled: RDD[(SpatialKey, MultibandTile)] = inputRdd.cutTiles(metadata)

    new ContextRDD(tiled, metadata)
  }

  def createSpaceTimeTileLayerRDD(
    tiles: Traversable[(Tile, ZonedDateTime)],
    tileLayout: TileLayout,
    cellType: CellType = IntConstantNoDataCellType)(implicit sc: SparkContext): TileLayerRDD[SpaceTimeKey] = {

    val extent = defaultCRS.worldExtent
    val layout = LayoutDefinition(extent, tileLayout)
    val keyBounds = {
      val GridBounds(colMin, rowMin, colMax, rowMax) = layout.mapTransform(extent)
      val minTime = tiles.minBy(_._2)._2
      val maxTime = tiles.maxBy(_._2)._2
      KeyBounds(SpaceTimeKey(colMin, rowMin, minTime), SpaceTimeKey(colMax, rowMax, maxTime))
    }
    val metadata = TileLayerMetadata(
      cellType,
      layout,
      extent,
      defaultCRS,
      keyBounds
    )

    val re = RasterExtent(
      extent = extent,
      cols = tileLayout.layoutCols,
      rows = tileLayout.layoutRows
    )

    val tileBounds = re.gridBoundsFor(extent)

    val tmsTiles = mutable.ListBuffer[(SpaceTimeKey, Tile)]()

    for( (tile, time) <- tiles) {
      val adjustedTile =
        if (tile.cols == tileLayout.totalCols.toInt &&
          tile.rows == tileLayout.totalRows.toInt) tile
        else CompositeTile.wrap(tile, tileLayout, cropped = false)

      tmsTiles ++=
        tileBounds.coordsIter.map { case (col, row) =>

          val targetRasterExtent =
            RasterExtent(
              extent = re.extentFor(GridBounds(col, row, col, row)),
              cols = tileLayout.tileCols,
              rows = tileLayout.tileRows
            )

          val subTile: Tile = adjustedTile.resample(extent, targetRasterExtent)

          (SpaceTimeKey(col, row, time), subTile)
        }
    }

    new ContextRDD(sc.parallelize(tmsTiles), metadata)
  }
}
