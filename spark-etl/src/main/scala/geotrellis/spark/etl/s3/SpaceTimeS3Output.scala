package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter

class SpaceTimeS3Output extends S3Output[SpaceTimeKey, Tile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](props("bucket"), props("key"), method)
}
