package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class MultibandSpatialAccumuloOutput extends AccumuloOutput[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(job.conf.outputProfile), job.outputProps("table"), strategy(job.outputProps)).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](job.conf.output.getKeyIndexMethod[SpatialKey])
}
