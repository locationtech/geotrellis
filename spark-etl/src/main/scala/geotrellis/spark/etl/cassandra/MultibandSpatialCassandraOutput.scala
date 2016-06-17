package geotrellis.spark.etl.cassandra

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class MultibandSpatialCassandraOutput extends CassandraOutput[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], job: EtlJob)(implicit sc: SparkContext) =
    CassandraLayerWriter(getInstance(job.outputCredentials), job.outputProps("keyspace"), job.outputProps("table")).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](method)
}
