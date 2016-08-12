package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.s3._

import org.apache.spark.SparkContext

class SpaceTimeMultibandS3Output extends S3Output[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    S3LayerWriter(conf.output.params("bucket"), conf.output.params("key")).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](conf.output.getKeyIndexMethod[SpaceTimeKey])
}
