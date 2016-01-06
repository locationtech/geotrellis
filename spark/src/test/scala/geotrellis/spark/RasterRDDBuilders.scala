package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark.tiling._
import geotrellis.spark.ingest._

import org.apache.spark._
import org.apache.spark.rdd.RDD

import com.github.nscala_time.time.Imports._
import scala.collection.mutable

trait RasterRDDBuilders {

  lazy val defaultCRS = LatLng

  /** Cuts the raster according to the layoutCols and layoutRows given.
    * Returns the raster that was used to fit inside the tile layout, which might
    * be smaller than the input raster
    *
    * Metadata mapTransform is accurate.
    */
  def createRasterRDD(
    input: Raster[Tile],
    layoutCols: Int,
    layoutRows: Int
  )(implicit sc: SparkContext): (Raster[Tile], RasterRDD[SpatialKey]) =
    createRasterRDD(input, layoutCols, layoutRows, defaultCRS)

  /** Cuts the raster according to the layoutCols and layoutRows given.
    * Returns the raser that was used to fit inside the tile layout, which might
    * be smaller than the input raster.
    *
    * Metadata mapTransform is accurate.
    */
  def createRasterRDD(
    raster: Raster[Tile],
    layoutCols: Int,
    layoutRows: Int,
    crs: CRS
  )(implicit sc: SparkContext): (Raster[Tile], RasterRDD[SpatialKey]) = {
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
    (resultRaster, createRasterRDD(sc, resultRaster, tileLayout, crs))
  }

  /** Cuts the tile according to the layoutCols and layoutRows given.
    * Returns the tile that was used to fit inside the tile layout, which might
    * be smaller than the input tile
    */
  def createRasterRDD(
    input: Tile,
    layoutCols: Int,
    layoutRows: Int
  )(implicit sc: SparkContext): (Tile, RasterRDD[SpatialKey]) =
    createRasterRDD(input, layoutCols, layoutRows, defaultCRS)

  /** Cuts the tile according to the layoutCols and layoutRows given.
    * Returns the tile that was used to fit inside the tile layout, which might
    * be smaller than the input tile
    */
  def createRasterRDD(
    input: Tile,
    layoutCols: Int,
    layoutRows: Int,
    crs: CRS
  )(implicit sc: SparkContext): (Tile, RasterRDD[SpatialKey]) = {
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

    (tile, createRasterRDD(sc, tile, tileLayout, crs))
  }

  def createRasterRDD(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout
  ): RasterRDD[SpatialKey] =
    createRasterRDD(sc, tile, tileLayout, defaultCRS)

  def createRasterRDD(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout,
    crs: CRS
  ): RasterRDD[SpatialKey] = {
    val extent = crs.worldExtent
    createRasterRDD(sc, Raster(tile, extent), tileLayout, crs)
  }

  def createRasterRDD(
    sc: SparkContext,
    raster: Raster[Tile],
    tileLayout: TileLayout
  ): RasterRDD[SpatialKey] =
    createRasterRDD(sc, raster, tileLayout, defaultCRS)

  def createRasterRDD(
    sc: SparkContext,
    raster: Raster[Tile],
    tileLayout: TileLayout,
    crs: CRS
  ): RasterRDD[SpatialKey] = {
    val layoutScheme = FloatingLayoutScheme(tileLayout.tileCols, tileLayout.tileRows)
    val inputRdd = sc.parallelize(Seq((ProjectedExtent(raster.extent, crs), raster.tile)))

    val (_, metadata) =
      RasterMetaData.fromRdd(inputRdd, crs, layoutScheme)(_.extent)

    val tiled: RDD[(SpatialKey, Tile)] = Tiler(inputRdd, metadata)

    new ContextRDD(tiled, metadata)
  }

  def createSpaceTimeRasterRDD(
    sc: SparkContext,
    tiles: Traversable[(Tile, DateTime)],
    tileLayout: TileLayout,
    cellType: CellType = TypeInt): RasterRDD[SpaceTimeKey] = {

    val extent = defaultCRS.worldExtent

    val metaData = RasterMetaData(
      cellType,
      LayoutDefinition(extent, tileLayout),
      extent,
      defaultCRS
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
        tileBounds.coords.map { case (col, row) =>

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

    new ContextRDD(sc.parallelize(tmsTiles), metaData)
  }
}
