package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import org.apache.spark.SparkContext

class SpaceTimeAccumuloInput extends AccumuloInput[SpaceTimeKey] {
  def reader(props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerReader[SpaceTimeKey, Tile, RasterRDD](getInstance(props))
}