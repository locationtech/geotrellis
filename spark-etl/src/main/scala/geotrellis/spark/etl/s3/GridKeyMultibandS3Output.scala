package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class GridKeyMultibandS3Output extends S3Output[GridKey, MultibandTile, RasterMetadata[GridKey]] {
  def writer(method: KeyIndexMethod[GridKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[GridKey, MultibandTile, RasterMetadata[GridKey]](method)
}
