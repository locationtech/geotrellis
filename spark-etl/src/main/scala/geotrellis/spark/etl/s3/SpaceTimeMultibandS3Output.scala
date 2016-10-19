package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.s3._

import org.apache.spark.SparkContext

class SpaceTimeMultibandS3Output extends S3Output[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) = {
    val path = getPath(conf.output.backend)
    S3LayerWriter(path.bucket, path.prefix).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](conf.output.getKeyIndexMethod[SpaceTimeKey])
  }
}
