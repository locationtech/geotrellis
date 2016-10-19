package geotrellis.spark.etl.cassandra

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra.CassandraLayerWriter

import org.apache.spark.SparkContext

class MultibandSpatialCassandraOutput extends CassandraOutput[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) = {
    val path = getPath(conf.output.backend)
    CassandraLayerWriter(getInstance(conf.outputProfile), path.keyspace, path.table).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](conf.output.getKeyIndexMethod[SpatialKey])
  }
}
