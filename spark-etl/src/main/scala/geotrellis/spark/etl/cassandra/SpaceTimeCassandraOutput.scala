package geotrellis.spark.etl.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpaceTimeCassandraOutput extends CassandraOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(job.conf.outputProfile), job.outputProps("keyspace"), job.outputProps("table")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](job.conf.output.getKeyIndexMethod[SpaceTimeKey])
}
