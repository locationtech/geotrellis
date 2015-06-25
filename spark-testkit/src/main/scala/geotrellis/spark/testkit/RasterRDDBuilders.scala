package geotrellis.spark.testkit

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.raster.mosaic._
import scala.reflect._
import org.apache.spark._
import com.github.nscala_time.time.Imports._
import scala.collection.mutable

trait RasterRDDBuilders {
  def defaultCrs = LatLng
  def defaultLayoutSceheme = ZoomedLayoutScheme(tileSize = 256)

  /** Create RasterRDD from single band GeoTiff */
  def createRasterRDD(path: String)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val tif = GeoTiffReader.readSingleBand(path)
    createRasterRDD(tif.tile, tif.extent, tif.crs)
  }

  def createRasterRDD(tile: Tile, extent: Extent, crs: CRS = LatLng, layoutScheme: LayoutScheme = ZoomedLayoutScheme(tileSize = 256))
  (implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val layoutLevel: LayoutLevel = layoutScheme.levelFor(crs.worldExtent, CellSize(extent, tile.cols, tile.rows))
    createRasterRDD(tile, extent, crs, layoutLevel.tileLayout) 
  }
  
  def createRasterRDD(tile: Tile, extent: Extent, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    createRasterRDD(tile, extent, defaultCrs, tileLayout)
  }

  def createRasterRDD(tile: Tile, extent: Extent, crs: CRS, tileLayout: TileLayout)(implicit sc: SparkContext): RasterRDD[SpatialKey] = {
    val worldExtent = crs.worldExtent

    val outputMapTransform = new MapKeyTransform(worldExtent, tileLayout.layoutCols, tileLayout.layoutRows)
    val metaData = RasterMetaData(tile.cellType, extent, crs, tileLayout)

    val tmsTiles = 
      for {
        (col, row) <- outputMapTransform(extent).coords // iterate over all tiles we should have for input extent in the world layout
      } yield {
        val key = SpatialKey(col, row)
        val worldTileExtent = outputMapTransform(key)
        val outTile = ArrayTile.empty(tile.cellType, tileLayout.tileCols, tileLayout.tileRows)       
        outTile.merge(worldTileExtent, extent, tile)
        key -> outTile
      }

    asRasterRDD(metaData) {
      sc.parallelize(tmsTiles)
    }
  }

  /** This method supports old style of tests, it should not be used */
  def createRasterRDD(sc: SparkContext, tile: Tile, tileLayout: TileLayout): RasterRDD[SpatialKey] = {
    createRasterRDD(tile, defaultCrs.worldExtent, tileLayout)(sc)
  }

  def createRasterRDD_Original(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout): RasterRDD[SpatialKey] = {

    val extent = defaultCrs.worldExtent

    val metaData = RasterMetaData(
      tile.cellType,
      extent,
      defaultCrs,
      tileLayout
    )

    val re = RasterExtent(
      extent = extent,
      cols = tileLayout.layoutCols,
      rows = tileLayout.layoutRows
    )

    val tileBounds = re.gridBoundsFor(extent)

    val adjustedTile =
      if (tile.cols == tileLayout.totalCols.toInt &&
        tile.rows == tileLayout.totalRows.toInt) tile
      else CompositeTile.wrap(tile, tileLayout, cropped = false)

    val tmsTiles =
      tileBounds.coords.map { case (col, row) =>

        val targetRasterExtent =
          RasterExtent(
            extent = re.extentFor(GridBounds(col, row, col, row)),
            cols = tileLayout.tileCols,
            rows = tileLayout.tileRows
          )

        val subTile: Tile = adjustedTile.resample(extent, targetRasterExtent)

        (SpatialKey(col, row), subTile)
      }


    asRasterRDD(metaData) {
      sc.parallelize(tmsTiles)
    }
  }

  // tile.resample and tile.merge does not produce the same results
  def createSpaceTimeRasterRDD(
    sc: SparkContext,
    tiles: Traversable[(Tile, DateTime)],
    tileLayout: TileLayout,
    cellType: CellType = TypeInt): RasterRDD[SpaceTimeKey] = {

    val extent = defaultCrs.worldExtent

    val metaData = RasterMetaData(
      cellType,
      extent,
      defaultCrs,
      tileLayout
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
    asRasterRDD(metaData) {
      sc.parallelize(tmsTiles)
    }
  }
}
