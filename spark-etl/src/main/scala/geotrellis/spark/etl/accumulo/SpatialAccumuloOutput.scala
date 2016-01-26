package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

class SpatialAccumuloOutput extends AccumuloOutput[SpatialKey] {
  def writer(props: Parameters) =
    AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData](getInstance(props), props("table"))
}
