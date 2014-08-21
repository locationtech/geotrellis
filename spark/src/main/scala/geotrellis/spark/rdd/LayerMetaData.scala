package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._
import geotrellis.proj4._

case class LayerMetaData(cellType: CellType, extent: Extent, crs: CRS, level: LayoutLevel, tileIndexScheme: TileIndexScheme) {
  lazy val transform = MapIndexTransform(MapGridTransform(crs, tileLayout.tileDimensions), tileIndexScheme(tileLayout.tileDimensions), tileLayout.tileDimensions)

  lazy val gridBounds = transform.mapToGrid(extent)
  lazy val tileIds = transform.gridToIndex(gridBounds)
  def tileLayout = level.tileLayout
}
