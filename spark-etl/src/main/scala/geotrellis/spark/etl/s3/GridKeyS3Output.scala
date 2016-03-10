package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class GridKeyS3Output extends S3Output[GridKey, Tile, RasterMetadata[GridKey]] {
  def writer(method: KeyIndexMethod[GridKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[GridKey, Tile, RasterMetadata[GridKey]](method)
}
