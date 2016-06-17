package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import org.apache.spark.SparkContext

class SpaceTimeS3Output extends S3Output[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](method)
}
