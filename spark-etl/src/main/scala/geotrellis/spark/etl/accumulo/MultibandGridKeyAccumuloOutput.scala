package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext

class MultibandGridKeyAccumuloOutput extends AccumuloOutput[GridKey, MultibandTile, RasterMetadata[GridKey]] {
  def writer(method: KeyIndexMethod[GridKey], props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(props), props("table")).writer[GridKey, MultibandTile, RasterMetadata[GridKey]](method)
}
