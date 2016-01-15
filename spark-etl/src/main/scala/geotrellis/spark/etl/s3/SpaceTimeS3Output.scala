package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._

class SpaceTimeS3Output extends S3Output[SpaceTimeKey] {
  def writer(props: Parameters) =
    S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](props("bucket"), props("key"))
}
