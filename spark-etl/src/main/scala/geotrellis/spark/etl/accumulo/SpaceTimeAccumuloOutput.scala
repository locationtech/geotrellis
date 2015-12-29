package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.json._

class SpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    AccumuloLayerWriter[SpaceTimeKey, Tile, RasterMetaData](getInstance(props),  props("table"), method)
}
