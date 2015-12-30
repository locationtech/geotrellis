package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloLayerReader
import org.apache.spark.SparkContext
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

class SpatialAccumuloInput extends AccumuloInput[SpatialKey] {
  def reader(props: Parameters)(implicit sc: SparkContext) =
    AccumuloLayerReader[SpatialKey, Tile, RasterMetaData](getInstance(props))
}
