package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class GridTimeKeyS3Output extends S3Output[GridTimeKey, Tile, RasterMetadata[GridTimeKey]] {
  def writer(method: KeyIndexMethod[GridTimeKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](method)
}
