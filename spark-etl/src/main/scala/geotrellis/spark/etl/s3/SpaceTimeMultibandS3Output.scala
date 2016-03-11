package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpaceTimeMultibandS3Output extends S3Output[SpaceTimeKey, MultibandTile, RasterMetaData[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[SpaceTimeKey, MultibandTile, RasterMetaData[SpaceTimeKey]](method)
}
