package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class SpatialS3Output extends S3Output[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    S3LayerWriter(props("bucket"), props("key")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](method)
}
