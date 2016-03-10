package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class SpatialMultibandS3Output extends S3Output[SpatialKey, MultibandTile, RasterMetaData[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[SpatialKey, MultibandTile, RasterMetaData[SpatialKey]](method)
}
