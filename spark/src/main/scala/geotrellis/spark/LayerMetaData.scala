package geotrellis.spark

import geotrellis.spark._
import geotrellis.spark.io.LayerId
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._
import geotrellis.proj4._

case class LayerMetaData(id: LayerId, rasterMetaData: RasterMetaData) {
  lazy val zoomLevel: ZoomLevel = ZoomLevel(id.zoom, tileLayout)
}

case class RasterMetaData(
  cellType: CellType,
  extent: Extent,
  crs: CRS,
  tileLayout: TileLayout
) {
  lazy val transform = MapIndexTransform(
    MapGridTransform(crs, tileLayout.tileDimensions),
    tileIndexScheme(tileLayout.tileDimensions),
    tileLayout.tileDimensions
  )

  lazy val gridBounds = transform.mapToGrid(extent)

  lazy val tileIds = transform.gridToIndex(gridBounds)

  lazy val count = tileIds.size

  lazy val cols = tileLayout.tileCols

  lazy val rows = tileLayout.tileRows
}
