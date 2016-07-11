package geotrellis.spark.etl.cassandra

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import org.apache.spark.SparkContext

class MultibandSpaceTimeCassandraOutput extends CassandraOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(job.conf.outputProfile), job.outputProps("keyspace"), job.outputProps("table")).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](job.conf.output.getKeyIndexMethod[SpaceTimeKey])
}
