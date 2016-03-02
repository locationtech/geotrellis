package geotrellis.spark.etl.s3

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.json._

import org.apache.spark.SparkContext

class SpaceTimeMultibandS3Output extends S3Output[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).keyBoundsComputingWriter[SpaceTimeKey, MultiBandTile, RasterMetaData[SpaceTimeKey]](method)
}
