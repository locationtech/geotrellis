package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import org.apache.spark.SparkContext

class SpaceTimeS3Output extends S3Output[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    S3LayerWriter(job.outputProps("bucket"), job.outputProps("key")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](job.input.ingestOptions.getKeyIndexMethod[SpaceTimeKey])
}
