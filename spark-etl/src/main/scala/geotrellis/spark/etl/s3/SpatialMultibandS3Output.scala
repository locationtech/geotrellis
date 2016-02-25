package geotrellis.spark.etl.s3

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

class SpatialMultibandS3Output extends S3Output[SpatialKey, MultiBandTile, RasterMetaData[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    S3LayerWriter[SpatialKey, MultiBandTile, RasterMetaData[SpatialKey]](props("bucket"), props("key"), method)
}