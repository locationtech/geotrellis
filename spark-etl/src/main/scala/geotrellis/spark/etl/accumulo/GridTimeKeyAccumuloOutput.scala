package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext

class GridTimeKeyAccumuloOutput extends AccumuloOutput[GridTimeKey, Tile, RasterMetadata[GridTimeKey]] {
  def writer(method: KeyIndexMethod[GridTimeKey], props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(props), props("table")).writer[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](method)
}
