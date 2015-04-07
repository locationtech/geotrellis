package geotrellis.spark.io

import geotrellis.raster.histogram.Histogram
import geotrellis.spark.RasterMetaData

/** This is a container that represents all that the catalog knows about a layer */
case class LayerMetaData (
  keyClass: String,
  rasterMetaData: RasterMetaData,
  histogram: Option[Histogram]
)
