package geotrellis.spark.etl.s3

import geotrellis.raster.Tile

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

class SpatialS3Output extends S3Output[SpatialKey] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    S3LayerWriter[SpatialKey, Tile, RasterMetaData](props("bucket"), props("key"), method)
}

