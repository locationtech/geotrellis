package geotrellis.spark.etl.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpatialCassandraOutput extends CassandraOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(job.outputCredentials), job.outputProps("keyspace"), job.outputProps("table")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](job.input.ingestOptions.getKeyIndexMethod[SpatialKey])
}
