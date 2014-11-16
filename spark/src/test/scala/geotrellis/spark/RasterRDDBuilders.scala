package geotrellis.spark

import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.spark.tiling._

import org.apache.spark._

// TODO: move this to testkit and break out new project for spark tests.
trait RasterRDDBuilders {

  lazy val defaultCellType = TypeInt

  lazy val defaultExtent = Extent(
    141.7066666666667,
    -18.373333333333342,
    142.56000000000003,
    -17.52000000000001
  )

  lazy val defaultCRS = LatLng

  lazy val defaultTileLayout = ZoomedLayoutScheme().levelFor(10).tileLayout

  lazy val defaultMetaData = RasterMetaData(
    defaultCellType,
    defaultExtent,
    defaultCRS,
    defaultTileLayout
  )

  def createRasterRDD(
    sc: SparkContext,
    raster: Tile,
    tileCols: Int,
    tileRows: Int,
    tilesX: Int,
    tilesY: Int): RasterRDD[SpatialKey] = {
    val rasterSize = raster.cols * raster.rows
    val tileSize = tileCols * tileRows

    if (rasterSize % tileSize != 0 || rasterSize / tileSize != tilesX * tilesY)
      throw new IllegalArgumentException("Bad input!")

    val crs = defaultMetaData.crs
    val tileLayout = defaultMetaData.tileLayout
    val re = RasterExtent(
      crs.worldExtent,
      tileLayout.layoutCols,
      tileLayout.layoutRows
    )
    val extent = defaultMetaData.extent
    val tileBounds = re.gridBoundsFor(extent)

    val rasterExtent =
      RasterExtent(
        extent = re.extentFor(tileBounds),
        cols = tileBounds.width * tileLayout.tileCols,
        rows = tileBounds.height * tileLayout.tileRows
      )

    val tmsTiles =
      tileBounds.coords.map { case (col, row) =>
        val targetRasterExtent =
          RasterExtent(
            extent = re.extentFor(GridBounds(col, row, col, row)),
            cols = tileLayout.tileCols,
            rows = tileLayout.tileRows
          )

        val subTile: Tile = raster.warp(rasterExtent.extent, targetRasterExtent)
        (SpatialKey(col, row), subTile)
      }

    asRasterRDD(defaultMetaData) {
      sc.parallelize(tmsTiles)
    }
  }

}
