package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class GridTimeKeyMultibandS3Output extends S3Output[GridTimeKey, MultibandTile, RasterMetadata[GridTimeKey]] {
  def writer(method: KeyIndexMethod[GridTimeKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[GridTimeKey, MultibandTile, RasterMetadata[GridTimeKey]](method)
}
