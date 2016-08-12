package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.s3.S3LayerWriter

import org.apache.spark.SparkContext

class SpatialS3Output extends S3Output[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    S3LayerWriter(conf.output.params("bucket"), conf.output.params("key")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](conf.output.getKeyIndexMethod[SpatialKey])
}
