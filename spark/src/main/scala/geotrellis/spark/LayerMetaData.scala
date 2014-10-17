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
  lazy val mapTransform = MapKeyTransform(crs, tileLayout.tileDimensions)

  def tileTransform(tileScheme: TileScheme): TileKeyTransform = tileScheme(tileLayout.tileCols, tileLayout.tileRows)
}
