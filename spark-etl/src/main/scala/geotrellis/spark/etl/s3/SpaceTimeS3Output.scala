package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

import scala.reflect._

class SpaceTimeS3Output extends S3Output[SpaceTimeKey] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](props("bucket"), props("key"), method)
}
