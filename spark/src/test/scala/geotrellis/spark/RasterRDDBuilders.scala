package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark.tiling._

import org.apache.spark._

import com.github.nscala_time.time.Imports._
import scala.collection.mutable

trait RasterRDDBuilders {

  lazy val defaultCRS = LatLng

  def createRasterRDD(
    sc: SparkContext,
    tile: Tile,
    tileLayout: TileLayout): RasterRDD[SpatialKey] = {

    val extent = defaultCRS.worldExtent

    val metaData = RasterMetaData(
      tile.cellType,
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


    new ContextRDD(sc.parallelize(tmsTiles), metaData)
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
