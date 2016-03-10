package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.accumulo.AccumuloLayerWriter

import org.apache.spark.SparkContext

class MultibandGridTimeKeyAccumuloOutput extends AccumuloOutput[GridTimeKey, MultibandTile, RasterMetadata[GridTimeKey]] {
  def writer(method: KeyIndexMethod[GridTimeKey], props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(props), props("table")).writer[GridTimeKey, MultibandTile, RasterMetadata[GridTimeKey]](method)
}
