package geotrellis.spark.io

import geotrellis.spark.tiling._
import geotrellis.spark.{LayerId, RasterMetaData}


/** Tuple to store RasterMetaData corresponding to a persisted LayerId */
case class LayerMetaData(id: LayerId, rasterMetaData: RasterMetaData) {
  lazy val layoutLevel: LayoutLevel = LayoutLevel(id.zoom, rasterMetaData.tileLayout)
}
