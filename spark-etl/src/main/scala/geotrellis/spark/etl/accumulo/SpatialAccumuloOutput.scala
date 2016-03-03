package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

import org.apache.spark.SparkContext

class SpatialAccumuloOutput extends AccumuloOutput[SpatialKey, Tile, RasterMetaData[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(props), props("table")).writer[SpatialKey, Tile, RasterMetaData[SpatialKey]](method)
}
