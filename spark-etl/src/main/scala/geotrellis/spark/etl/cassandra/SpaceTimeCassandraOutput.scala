package geotrellis.spark.etl.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpaceTimeCassandraOutput extends CassandraOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(props), props("table")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](method)
}
