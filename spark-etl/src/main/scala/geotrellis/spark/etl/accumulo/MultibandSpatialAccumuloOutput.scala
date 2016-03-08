package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

class MultibandSpatialAccumuloOutput extends AccumuloOutput[SpatialKey, MultiBandTile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    AccumuloLayerWriter[SpatialKey, MultiBandTile, RasterMetaData](getInstance(props), props("table"), method)
}
