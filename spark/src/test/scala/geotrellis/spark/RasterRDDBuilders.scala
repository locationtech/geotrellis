package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.spark.tiling._

import org.apache.spark._

// TODO: move this to testkit and break out new project for spark tests.
trait RasterRDDBuilders {

  lazy val defaultCRS = LatLng

  def createRasterRDD(
    sc: SparkContext,
    raster: Tile,
    tileCols: Int,
    tileRows: Int,
    tilesX: Int,
    tilesY: Int,
    cellType: CellType = TypeInt): RasterRDD[SpatialKey] = {
    val rasterSize = raster.cols * raster.rows
    val tileSize = tileCols * tileRows

    if (rasterSize % tileSize != 0 || rasterSize / tileSize != tilesX * tilesY)
      throw new IllegalArgumentException("Bad input!")

    val tileLayout = TileLayout(tilesX, tilesY, tileCols, tileRows)

    val extent = defaultCRS.worldExtent

    val metaData = RasterMetaData(
      cellType,
      extent,
      defaultCRS,
      tileLayout
    )

    val re = RasterExtent(
      extent = extent,
      cols = tileLayout.layoutCols,
      rows = tileLayout.layoutRows
    )

    val tileBounds = re.gridBoundsFor(extent)

    val tmsTiles =
      tileBounds.coords.map { case (col, row) =>
        val targetRasterExtent =
          RasterExtent(
            extent = re.extentFor(GridBounds(col, row, col, row)),
            cols = tileLayout.tileCols,
            rows = tileLayout.tileRows
          )

        val subTile: Tile = raster.warp(extent, targetRasterExtent)

        (SpatialKey(col, row), subTile)
      }

    asRasterRDD(metaData) {
      sc.parallelize(tmsTiles)
    }
  }

}
