package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.json._

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    AccumuloLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]](getInstance(props), props("table"), method)
}
