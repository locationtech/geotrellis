package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.json._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter

import org.apache.spark.SparkContext

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(props), props("table")).keyBoundsComputingWriter[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]](method)
}
