package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpaceTimeMultibandS3Output extends S3Output[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    S3LayerWriter(job.outputProps("bucket"), job.outputProps("key")).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](job.input.ingestOptions.getKeyIndexMethod[SpaceTimeKey])
}
