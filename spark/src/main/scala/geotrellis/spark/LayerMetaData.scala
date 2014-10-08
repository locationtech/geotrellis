package geotrellis.spark

import geotrellis.spark._
import geotrellis.spark.io.LayerId
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._
import geotrellis.proj4._

case class LayerMetaData(
  name: String,
  cellType: CellType,
  extent: Extent,
  crs: CRS,
  level: LayoutLevel,
  tileIndexScheme: TileIndexScheme) {
  lazy val id: LayerId = LayerId(name, level.id)

  lazy val transform = MapIndexTransform(
    MapGridTransform(crs, tileLayout.tileDimensions),
    tileIndexScheme(tileLayout.tileDimensions),
    tileLayout.tileDimensions
  )

  lazy val gridBounds = transform.mapToGrid(extent)

  lazy val tileIds = transform.gridToIndex(gridBounds)

  lazy val count = tileIds.size

  lazy val tileLayout = level.tileLayout

  lazy val cols = tileLayout.tileCols

  lazy val rows = tileLayout.tileRows

}
