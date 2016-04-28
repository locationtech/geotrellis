package geotrellis.spark.etl.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpatialCassandraOutput extends CassandraOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(props), props("table")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](method)
}
