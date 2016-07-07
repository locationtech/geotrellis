package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import org.apache.spark.SparkContext

class SpatialMultibandS3Output extends S3Output[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    S3LayerWriter(job.outputProps("bucket"), job.outputProps("key")).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](job.input.ingestOptions.getKeyIndexMethod[SpatialKey])
}
