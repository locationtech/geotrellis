package geotrellis.spark.rdd

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._

case class LayerMetaData(cellType: CellType, extent: Extent, zoomLevel: ZoomLevel)

